package com.theoryinpractise.halbuilder;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.theoryinpractise.halbuilder.spi.ReadableResource;
import com.theoryinpractise.halbuilder.spi.Resource;
import com.theoryinpractise.halbuilder.spi.ResourceException;
import com.theoryinpractise.halbuilder.spi.Serializable;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;

import static org.fest.assertions.Assertions.assertThat;

public class RenderingTest {

    private ResourceFactory resourceFactory = new ResourceFactory("https://example.com/api/")
                                                      .withNamespace("ns", "/apidocs/accounts")
                                                      .withNamespace("role", "/apidocs/roles");

    private String exampleXml;
    private String exampleJson;
    private String exampleWithSubresourceXml;
    private String exampleWithSubresourceJson;
    private String exampleWithMultipleSubresourcesXml;
    private String exampleWithMultipleSubresourcesJson;

    @BeforeMethod
    public void setup() throws IOException {
        exampleXml = Resources.toString(RenderingTest.class.getResource("example.xml"), Charsets.UTF_8)
                              .trim().replaceAll("\n", "\r\n");
        exampleJson = Resources.toString(RenderingTest.class.getResource("example.json"), Charsets.UTF_8)
                               .trim();
        exampleWithSubresourceXml = Resources.toString(RenderingTest.class.getResource("exampleWithSubresource.xml"), Charsets.UTF_8)
                                             .trim().replaceAll("\n", "\r\n");
        exampleWithSubresourceJson = Resources.toString(RenderingTest.class.getResource("exampleWithSubresource.json"), Charsets.UTF_8)
                                              .trim();
        exampleWithMultipleSubresourcesXml = Resources.toString(RenderingTest.class.getResource("exampleWithMultipleSubresources.xml"), Charsets.UTF_8)
                                                      .trim().replaceAll("\n", "\r\n");
        exampleWithMultipleSubresourcesJson = Resources.toString(RenderingTest.class.getResource("exampleWithMultipleSubresources.json"), Charsets.UTF_8)
                                                       .trim();
    }


    private Resource newBaseResource(final Resource resource) {
        return resource.withLink("/api/customer/1234", "ns:parent",
                                        Optional.<Predicate<ReadableResource>>absent(),
                                        Optional.of("bob"),
                                        Optional.of("The Parent"),
                                        Optional.of("en"));
    }

    private Resource newBaseResource(final URI uri) {
        return newBaseResource(resourceFactory.newResource(uri));

    }

    private Resource newBaseResource(final String href) {
        return newBaseResource(resourceFactory.newResource(href));
    }

    @Test
    public void testFactoryWithLinks() {

        ResourceFactory resourceFactory = new ResourceFactory("https://example.com/api/")
                                                  .withLink("/home", "home");

        Resource resource = resourceFactory.newResource("/");

        assertThat(resource.getCanonicalLinks()).hasSize(2);
        assertThat(resource.getLinksByRel("home")).hasSize(1);
        assertThat(resource.getLinksByRel("home").iterator().next().toString()).isEqualTo("<link rel=\"home\" href=\"https://example.com/home\"/>");

    }

    @Test(expectedExceptions = ResourceException.class)
    public void testFactoryWithDuplicateNamespaces() {
        new ResourceFactory()
                .withNamespace("home", "https://example.com/api/")
                .withNamespace("home", "https://example.com/api/");
    }


    @Test
    public void testUriBuilderHal() {

        URI path = UriBuilder.fromPath("customer/{id}").buildFromMap(ImmutableMap.of("id", "123456"));

        ReadableResource party = newBaseResource(path)
                                           .withLink("?users", "ns:users")
                                           .withProperty("id", 123456)
                                           .withProperty("age", 33)
                                           .withProperty("name", "Example Resource")
                                           .withProperty("optional", Boolean.TRUE)
                                           .withProperty("expired", Boolean.FALSE);

        assertThat(party.getResourceLink().getHref()).isEqualTo("https://example.com/api/customer/123456");
        assertThat(party.renderContent(ResourceFactory.HAL_XML)).isEqualTo(exampleXml);
        assertThat(party.renderContent(ResourceFactory.HAL_JSON)).isEqualTo(exampleJson);

    }

    @Test
    public void testCustomerHal() {

        ReadableResource party = newBaseResource("customer/123456")
                .withLink("?users", "ns:users")
                .withProperty("id", 123456)
                .withProperty("age", 33)
                .withProperty("name", "Example Resource")
                .withProperty("optional", Boolean.TRUE)
                .withProperty("expired", Boolean.FALSE);

        assertThat(party.getResourceLink().getHref()).isEqualTo("https://example.com/api/customer/123456");
        assertThat(party.renderContent(ResourceFactory.HAL_XML)).isEqualTo(exampleXml);
        assertThat(party.renderContent(ResourceFactory.HAL_JSON)).isEqualTo(exampleJson);

    }

    @Test
    public void testWithSerializable() {

        ReadableResource party = newBaseResource("customer/123456")
                .withLink("?users", "ns:users")
                .withSerializable(new Serializable() {
                    public void serializeResource(Resource resource) {
                        resource.withProperty("id", 123456)
                                .withProperty("age", 33)
                                .withProperty("name", "Example Resource")
                                .withProperty("optional", Boolean.TRUE)
                                .withProperty("expired", Boolean.FALSE);
                    }
                });

        assertThat(party.renderContent(ResourceFactory.HAL_XML)).isEqualTo(exampleXml);
        assertThat(party.renderContent(ResourceFactory.HAL_JSON)).isEqualTo(exampleJson);

    }


    @Test
    public void testHalWithBean() {

        ReadableResource party = newBaseResource("customer/123456")
                .withLink("?users", "ns:users")
                .withBean(new Customer(123456, "Example Resource", 33));

        assertThat(party.renderContent(ResourceFactory.HAL_XML)).isEqualTo(exampleXml);
        assertThat(party.renderContent(ResourceFactory.HAL_JSON)).isEqualTo(exampleJson);

    }

    @Test
    public void testHalWithFields() {

        ReadableResource party = newBaseResource("customer/123456")
                .withLink("?users", "ns:users")
                .withFields(new OtherCustomer(123456, "Example Resource", 33));

        assertThat(party.renderContent(ResourceFactory.HAL_XML)).isEqualTo(exampleXml);
        assertThat(party.renderContent(ResourceFactory.HAL_JSON)).isEqualTo(exampleJson);

    }

    @Test
    public void testHalWithSubResources() {

        ReadableResource party = newBaseResource("customer/123456")
                .withLink("?users", "ns:users")
                .withSubresource("ns:user role:admin", resourceFactory
                                                               .newResource("/user/11")
                                                               .withProperty("id", 11)
                                                               .withProperty("name", "Example User")
                                                               .withProperty("expired", false)
                                                               .withProperty("age", 32)
                                                               .withProperty("optional", true));

        assertThat(party.renderContent(ResourceFactory.HAL_XML)).isEqualTo(exampleWithSubresourceXml);
        assertThat(party.renderContent(ResourceFactory.HAL_JSON)).isEqualTo(exampleWithSubresourceJson);

    }

    @Test
    public void testHalWithBeanSubResource() {

        ReadableResource party = newBaseResource("customer/123456")
                .withLink("?users", "ns:users")
                .withBeanBasedSubresource("ns:user role:admin", "/user/11", new Customer(11, "Example User", 32));

        assertThat(party.renderContent(ResourceFactory.HAL_XML)).isEqualTo(exampleWithSubresourceXml);
        assertThat(party.renderContent(ResourceFactory.HAL_JSON)).isEqualTo(exampleWithSubresourceJson);

    }

    @Test
    public void testHalWithBeanMultipleSubResources() {

        ReadableResource party = newBaseResource("customer/123456")
                .withLink("?users", "ns:users")
                .withBeanBasedSubresource("ns:user role:admin", "/user/11", new Customer(11, "Example User", 32))
                .withBeanBasedSubresource("ns:user role:admin", "/user/12", new Customer(12, "Example User", 32));

        assertThat(party.renderContent(ResourceFactory.HAL_XML)).isEqualTo(exampleWithMultipleSubresourcesXml);
        assertThat(party.renderContent(ResourceFactory.HAL_JSON)).isEqualTo(exampleWithMultipleSubresourcesJson);

    }

    public static class OtherCustomer {
        public final Integer id;
        public final String name;
        public final Integer age;
        public final Boolean expired = false;
        public final Boolean optional = true;

        public OtherCustomer(Integer id, String name, Integer age) {
            this.id = id;
            this.name = name;
            this.age = age;
        }
    }

    public static class Customer {
        private Integer id;
        private String name;
        private Integer age;
        private Boolean expired = false;
        private Boolean optional = true;

        public Customer(Integer id, String name, Integer age) {
            this.id = id;
            this.name = name;
            this.age = age;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Boolean getExpired() {
            return expired;
        }

        public void setExpired(Boolean expired) {
            this.expired = expired;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public Boolean getOptional() {
            return optional;
        }

        public void setOptional(Boolean optional) {
            this.optional = optional;
        }
    }

}
