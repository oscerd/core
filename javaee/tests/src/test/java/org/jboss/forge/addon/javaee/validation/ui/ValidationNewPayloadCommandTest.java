package org.jboss.forge.addon.javaee.validation.ui;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.jboss.forge.addon.javaee.JavaEEPackageConstants.DEFAULT_CONSTRAINT_PACKAGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.addon.javaee.ProjectHelper;
import org.jboss.forge.addon.javaee.validation.ValidationFacet;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.shell.test.ShellTest;
import org.jboss.forge.addon.ui.controller.CommandController;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.result.Failed;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.test.UITestHarness;
import org.jboss.forge.arquillian.AddonDependencies;
import org.jboss.forge.arquillian.AddonDependency;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.forge.roaster.model.JavaClass;
import org.jboss.forge.roaster.model.Visibility;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the creation of a new Bean Validation payload
 *
 * @author <a href="antonio.goncalves@gmail.com">Antonio Goncalves</a>
 */
@RunWith(Arquillian.class)
public class ValidationNewPayloadCommandTest
{
   @Deployment
   @AddonDependencies({
            @AddonDependency(name = "org.jboss.forge.addon:ui-test-harness"),
            @AddonDependency(name = "org.jboss.forge.addon:shell-test-harness"),
            @AddonDependency(name = "org.jboss.forge.addon:javaee"),
            @AddonDependency(name = "org.jboss.forge.addon:maven"),
            @AddonDependency(name = "org.jboss.forge.furnace.container:cdi")
   })
   public static AddonArchive getDeployment()
   {
      return ShrinkWrap.create(AddonArchive.class).addBeansXML().addClass(ProjectHelper.class);
   }

   @Inject
   private UITestHarness uiTestHarness;

   @Inject
   private ShellTest shellTest;

   @Inject
   private ProjectHelper projectHelper;

   private Project project;

   @Before
   public void setUp()
   {
      project = projectHelper.createJavaLibraryProject();
      projectHelper.installValidation(project);
   }

   @Test
   public void checkCommandMetadata() throws Exception
   {
      try (CommandController controller = uiTestHarness.createCommandController(ValidationNewPayloadCommand.class,
               project.getRoot()))
      {
         controller.initialize();
         // Checks the command metadata
         assertTrue(controller.getCommand() instanceof ValidationNewPayloadCommand);
         assertTrue(controller.getCommand() instanceof AbstractValidationCommand);
         UICommandMetadata metadata = controller.getMetadata();
         assertEquals("Constraint: New Payload", metadata.getName());
         assertEquals("Java EE", metadata.getCategory().getName());
         assertEquals("Bean Validation", metadata.getCategory().getSubCategory().getName());
         assertEquals(3, controller.getInputs().size());
         assertTrue(controller.hasInput("named"));
         assertTrue(controller.hasInput("targetPackage"));
         assertTrue(controller.hasInput("overwrite"));
         assertTrue(controller.getValueFor("targetPackage").toString().endsWith(DEFAULT_CONSTRAINT_PACKAGE));
      }
   }

   @Test
   public void checkCommandShell() throws Exception
   {
      shellTest.getShell().setCurrentResource(project.getRoot());
      Result result = shellTest.execute(("constraint-new-payload --named Dummy"), 10, TimeUnit.SECONDS);

      Assert.assertThat(result, not(instanceOf(Failed.class)));
      Assert.assertTrue(project.hasFacet(ValidationFacet.class));
   }

   @Test
   public void testCreateNewPayload() throws Exception
   {
      try (CommandController controller = uiTestHarness.createCommandController(ValidationNewPayloadCommand.class,
               project.getRoot()))
      {
         controller.initialize();
         controller.setValueFor("named", "MyPayload");
         Assert.assertTrue(controller.isValid());
         Assert.assertTrue(controller.canExecute());
         Result result = controller.execute();
         Assert.assertThat(result, is(not(instanceOf(Failed.class))));
      }

      JavaSourceFacet facet = project.getFacet(JavaSourceFacet.class);
      String path = facet.getBasePackage() + "." + DEFAULT_CONSTRAINT_PACKAGE;
      JavaResource javaResource = facet.getJavaResource(path + ".MyPayload");
      Assert.assertNotNull(javaResource);
      Assert.assertThat(javaResource.getJavaType(), is(instanceOf(JavaClass.class)));
      JavaClass<?> payload = javaResource.getJavaType();
      Assert.assertEquals("MyPayload", payload.getName());
      Assert.assertEquals(Visibility.PUBLIC, payload.getVisibility());
      Assert.assertEquals(0, payload.getProperties().size());
      Assert.assertEquals(0, payload.getAnnotations().size());
      Assert.assertEquals(0, payload.getMembers().size());
      Assert.assertEquals(0, payload.getMethods().size());
      Assert.assertEquals(1, payload.getInterfaces().size());
      Assert.assertEquals("javax.validation.Payload", payload.getInterfaces().get(0));
   }
}
