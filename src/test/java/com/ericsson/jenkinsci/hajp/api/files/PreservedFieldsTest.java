package com.ericsson.jenkinsci.hajp.api.files;

import org.junit.Assert;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;

public class PreservedFieldsTest {

    private PreservedFields preservedFields;

    @Test public void testPreservedFields() throws JAXBException {
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
            + "<preservedFields>\n" + "    <jobs>credentialsId</jobs>\n" + "</preservedFields>";

        preservedFields = new PreservedFields();
        preservedFields.getJobs().add("credentialsId");
        preservedFields.getJobs().add("credentialsId");

        JAXBContext jaxbContext = JAXBContext.newInstance(PreservedFields.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        StringWriter writer = new StringWriter();
        jaxbMarshaller.marshal(preservedFields, writer);
        jaxbMarshaller.marshal(preservedFields, System.out);

        Assert.assertEquals(expected, writer.toString().trim());
    }
}
