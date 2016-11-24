package com.company.cejt.core

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.app.importexport.EntityImportExportService;
import com.haulmont.cuba.core.app.importexport.EntityImportView;
import com.haulmont.cuba.core.app.importexport.ReferenceImportBehaviour;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.Resources;
import com.haulmont.cuba.core.sys.AppContext
import com.haulmont.cuba.security.app.Authentication
import com.haulmont.cuba.security.entity.Constraint
import com.haulmont.cuba.security.entity.Group
import com.haulmont.cuba.security.entity.GroupHierarchy
import com.haulmont.cuba.security.entity.Permission
import com.haulmont.cuba.security.entity.Role
import com.haulmont.cuba.security.entity.SessionAttribute;
import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternUtils;

import javax.annotation.ManagedBean;
import javax.inject.Inject

/**
 * Imports test data through JSON files in /data/seed / data/test directories
 *
 * The files should have the following naming convention:
 *
 * #nr#-#entityName#-#description#.zip
 *
 * - #nr# is just for ordering purposes
 * - #entityName# is should be the value of the @Entity annotation, where the '$' is replaced by a '_'
 *   - examples: 'sec_Group', 'cejt_Customer'
 */
@ManagedBean("cejt_JsonDataImporter")
public class JsonDataImporter implements AppContext.Listener {

    private final Logger log = LoggerFactory.getLogger(JsonDataImporter.class);

    public static String TESTDATA_FILE_PATTERN = "data/test/*.zip";
    public static String SEEDDATA_FILE_PATTERN = "data/seed/*.zip";

    @Inject
    Resources resources;

    @Inject
    Metadata metadata;

    @Inject
    EntityImportExportService entityImportExportService;

    @Inject
    protected Authentication authentication


    public JsonDataImporter() {
        AppContext.addListener(this);
    }


    @Override
    public void applicationStarted() {
        importSeedData();
        importTestdata();

        clearConfigurationCache();
    }

    public void importTestdata() {
        importData(TESTDATA_FILE_PATTERN)
    }

    public void importSeedData() {
        importData(SEEDDATA_FILE_PATTERN)
    }

    protected void importData(String filePattern) {
        authentication.begin();
        try {
            try {

                log.info("Loading resources for: " + filePattern);
                Resource[] allZipResources = loadResources(filePattern);

                def sortedZipResources = allZipResources.sort { it.filename }

                sortedZipResources.each { resource ->
                    importTestdataForResource(resource)
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } finally {
            authentication.end();
        }
    }


    protected void clearConfigurationCache() {
        configStorageAPI.clearCache()
    }

    Resource[] loadResources(String pattern) throws IOException {
        return ResourcePatternUtils.getResourcePatternResolver(resources).getResources(pattern);
    }

    protected void importTestdataForResource(Resource resource) {
        byte[] zipBytes = IOUtils.toByteArray(resource.getInputStream());

        def entityClass = determineEntityClass(resource)


        if (entityClass) {
            EntityImportView entityImportView = createEntityImportViewForEntityClass(entityClass)
            entityImportExportService.importEntities(zipBytes, entityImportView);

            log.info("Import successful for: " + resource.filename)
        }
        else {
            log.warn("Class could not be found - import skipped for: " + resource.filename)
        }
    }

    protected MetaClass determineEntityClass(Resource resource) {

        try {
            String filename = resource.getFilename();
            String[] filenameParts = filename.split("-");
            String classNameWithExtension = filenameParts[1];
            String className = classNameWithExtension - ".zip"


            def cubaMetaClassString = className.replaceAll('_', '\\$')
            metadata.getClassNN(cubaMetaClassString)
        }
        catch (ClassNotFoundException e) {
            return null
        }

    }


    protected EntityImportView createEntityImportViewForEntityClass(MetaClass entityClass) {
        if (entityClass.getName() == 'sec$Group') {
            return createGroupsImportView()
        } else if (entityClass.getName() == 'sec$Role') {
            return createRolesImportView()
        } else {
            return createEntityImportView(entityClass);
        }
    }

    protected EntityImportView createEntityImportView(MetaClass metaClass) {

        EntityImportView entityImportView = new EntityImportView(metaClass.getJavaClass());
        for (MetaProperty metaProperty : metaClass.getProperties()) {
            switch (metaProperty.getType()) {
                case MetaProperty.Type.DATATYPE:
                case MetaProperty.Type.ENUM:
                    if (!metaProperty.annotatedElement.isAnnotationPresent(com.haulmont.chile.core.annotations.MetaProperty)) {
                        entityImportView.addProperty(metaProperty.getName());
                    }
                    break;
                case MetaProperty.Type.ASSOCIATION:
                case MetaProperty.Type.COMPOSITION:
                    if (!metaProperty.getRange().getCardinality().isMany()) {
                        entityImportView.addProperty(metaProperty.getName(), ReferenceImportBehaviour.IGNORE_MISSING);
                    }
                    break;
                default:
                    throw new IllegalStateException("unknown property type");
            }
        }
        return entityImportView;
    }

    protected EntityImportView createGroupsImportView() {
        return new EntityImportView(Group.class)
                .addLocalProperties()
                .addProperty("parent", ReferenceImportBehaviour.IGNORE_MISSING)
                .addProperty("hierarchyList", new EntityImportView(GroupHierarchy.class)
                .addLocalProperties()
                .addProperty("parent", ReferenceImportBehaviour.IGNORE_MISSING))
                .addProperty("sessionAttributes", new EntityImportView(SessionAttribute.class).addLocalProperties())
                .addProperty("constraints", new EntityImportView(Constraint.class).addLocalProperties());
    }

    protected EntityImportView createRolesImportView() {
        return new EntityImportView(Role.class)
                .addLocalProperties()
                .addProperty("permissions", new EntityImportView(Permission.class).addLocalProperties());
    }

    @Override
    public void applicationStopped() {

    }
}
