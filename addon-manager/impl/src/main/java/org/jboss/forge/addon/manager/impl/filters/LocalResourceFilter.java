package org.jboss.forge.addon.manager.impl.filters;

import org.jboss.forge.addon.dependencies.DependencyNode;
import org.jboss.forge.addon.dependencies.collection.DependencyNodeUtil;
import org.jboss.forge.container.util.Predicate;

/**
 * Figures out if a {@link DependencyNode} should be treated as a JAR or as an Addon. Also if that dependency is a
 * direct dependency of the current addon. If it is a runtime required resource belonging to the given Addon, then it
 * should be accepted and packaged as a JAR in the addon deployment. Otherwise, ignore it
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class LocalResourceFilter implements Predicate<DependencyNode>
{
   private final DependencyNode addon;

   public LocalResourceFilter(DependencyNode addon)
   {
      this.addon = addon;
   }

   @Override
   public boolean accept(DependencyNode node)
   {
      if (isDependencyAddon(node) || !shouldPackageResource(node))
      {
         return false;
      }
      return true;
   }

   public boolean isDependencyAddon(DependencyNode node)
   {
      return (DependencyNodeUtil.isForgeAddon(node.getDependency().getCoordinate()) && !node.equals(addon));
   }

   public boolean shouldPackageResource(DependencyNode node)
   {
      return node.getDependency().getScopeType() == null
               || node.getDependency().getScopeType().isEmpty()
               || "compile".equals(node.getDependency().getScopeType())
               || "runtime".equals(node.getDependency().getScopeType());
   }

}