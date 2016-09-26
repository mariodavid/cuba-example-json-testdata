package com.company.cejt.core;

import com.company.cejt.entity.Customer;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.app.importexport.EntityImportExportService;
import com.haulmont.cuba.core.app.importexport.EntityImportView;
import com.haulmont.cuba.core.app.importexport.ReferenceImportBehaviour;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.Resources;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.security.app.Authenticated;
import com.haulmont.cuba.security.app.Authentication;
import org.apache.commons.io.IOUtils;

import javax.annotation.ManagedBean;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;

@ManagedBean("cejt_TestdataImporter")
public class TestdataImporter implements AppContext.Listener {


    @Inject
    Resources resources;

    @Inject
    Metadata metadata;

    @Inject
    EntityImportExportService entityImportExportService;

    @Inject
    protected Authentication authentication;


    public TestdataImporter() {
        AppContext.addListener(this);
    }


    @Override
    public void applicationStarted() {
        importTestdata();
    }

    public void importTestdata() {
        authentication.begin();
        try {
            try {
                InputStream stream = resources.getResourceAsStream("com/company/cejt/core/testdata/Customer.zip");
                if (stream != null) {
                    byte[] zipBytes = IOUtils.toByteArray(stream);
                    EntityImportView entityImportView = createEntityImportView(metadata.getClass(Customer.class));
                    entityImportExportService.importEntities(zipBytes,entityImportView);
                }
                else {
                    throw new RuntimeException("did not work");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } finally {
            authentication.end();
        }



    }

    protected EntityImportView createEntityImportView(MetaClass metaClass) {
        EntityImportView entityImportView = new EntityImportView(metaClass.getJavaClass());
        for (MetaProperty metaProperty : metaClass.getProperties()) {
            switch (metaProperty.getType()) {
                case DATATYPE:
                case ENUM:
                    entityImportView.addProperty(metaProperty.getName());
                    break;
                case ASSOCIATION:
                case COMPOSITION:
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

    @Override
    public void applicationStopped() {

    }
}
