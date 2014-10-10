package org.springsource.ide.eclipse.gradle.core.launch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.StandardClasspathProvider;
import org.gradle.tooling.model.ExternalDependency;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.FastOperationFailedException;

@SuppressWarnings("restriction")
public class GradleRuntimeClasspathProvider extends StandardClasspathProvider {
	
	private static final String GRADLE_CLASSPATH_PROVIDER = "org.springsource.ide.eclipse.gradle.core.launch.classpathProvider";
	
	private static final String GRADLE_SOURCEPATH_PROVIDER = "";
	
	private static final IPath TEST_SOURCE_PATH = new Path("src/test/java");
	
	private static final IPath TEST_RESOURCES_PATH = new Path("src/test/resources");

	private static final List<String> SUPPORTED_TYPES = Arrays
			.asList(new String[]{
					IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION,
					JUnitLaunchConfigurationConstants.ID_JUNIT_APPLICATION});
	  
	public static boolean isSupportedType(String id) {
		return SUPPORTED_TYPES.contains(id);
	}

	public static void enable(ILaunchConfiguration config) throws CoreException {
		if (config instanceof ILaunchConfigurationWorkingCopy) {
			enable((ILaunchConfigurationWorkingCopy) config);
		} else {
			ILaunchConfigurationWorkingCopy wc = config.getWorkingCopy();
			enable(wc);
			wc.doSave();
		}
	}

	private static void enable(ILaunchConfigurationWorkingCopy wc) {
		wc.setAttribute(
				IJavaLaunchConfigurationConstants.ATTR_CLASSPATH_PROVIDER,
				GRADLE_CLASSPATH_PROVIDER);
		wc.setAttribute(
				IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER,
				GRADLE_SOURCEPATH_PROVIDER);
	}

	public static void disable(ILaunchConfiguration config)
			throws CoreException {
		ILaunchConfigurationWorkingCopy wc = config.getWorkingCopy();
		wc.setAttribute(
				IJavaLaunchConfigurationConstants.ATTR_CLASSPATH_PROVIDER,
				(String) null);
		wc.setAttribute(
				IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER,
				(String) null);
		wc.doSave();
	}

	public static void enable(IProject project) throws CoreException {
		for (ILaunchConfiguration config : getLaunchConfiguration(project)) {
			if (isSupportedType(config.getType().getIdentifier())) {
				enable(config);
			}
		}
	}

	public static void disable(IProject project) throws CoreException {
		for (ILaunchConfiguration config : getLaunchConfiguration(project)) {
			if (isSupportedType(config.getType().getIdentifier())) {
				disable(config);
			}
		}
	}

	private static List<ILaunchConfiguration> getLaunchConfiguration(
			IProject project) throws CoreException {
		ArrayList<ILaunchConfiguration> result = new ArrayList<ILaunchConfiguration>();
		ILaunchManager launchManager = DebugPlugin.getDefault()
				.getLaunchManager();
		ILaunchConfiguration[] configurations = launchManager
				.getLaunchConfigurations();
		for (ILaunchConfiguration config : configurations) {
			String projectName = config.getAttribute(
					IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
					(String) null);
			if (project.getName().equals(projectName)) {
				result.add(config);
			}
		}
		return result;
	}
	
	@Override
	public IRuntimeClasspathEntry[] computeUnresolvedClasspath(
			final ILaunchConfiguration configuration) throws CoreException {
		boolean useDefault = configuration.getAttribute(
				IJavaLaunchConfigurationConstants.ATTR_DEFAULT_CLASSPATH, true);
		if (useDefault) {
			IJavaProject javaProject = JavaRuntime
					.getJavaProject(configuration);
			IRuntimeClasspathEntry jreEntry = JavaRuntime
					.computeJREEntry(configuration);
			IRuntimeClasspathEntry projectEntry = JavaRuntime
					.newProjectRuntimeClasspathEntry(javaProject);

			if (jreEntry == null) {
				return new IRuntimeClasspathEntry[]{projectEntry};
			}

			return new IRuntimeClasspathEntry[]{jreEntry, projectEntry};
		}

		return recoverRuntimePath(configuration,
				IJavaLaunchConfigurationConstants.ATTR_CLASSPATH);
//		return super.computeUnresolvedClasspath(configuration);
	}
	  
	@Override
	public IRuntimeClasspathEntry[] resolveClasspath(
			IRuntimeClasspathEntry[] entries, ILaunchConfiguration configuration)
			throws CoreException {
	    Set<IRuntimeClasspathEntry> all = new LinkedHashSet<IRuntimeClasspathEntry>(entries.length);
	    for(IRuntimeClasspathEntry entry : entries) {
	      if(entry.getType() == IRuntimeClasspathEntry.PROJECT) {
	        IJavaProject javaProject = JavaRuntime.getJavaProject(configuration);
	        if(javaProject.getPath().equals(entry.getPath())) {
	          addProjectEntries(all, entry, configuration);
	        } else {
	          addStandardClasspathEntries(all, entry, configuration);
	        }
	      } else {
	        addStandardClasspathEntries(all, entry, configuration);
	      }
	    }
	    return all.toArray(new IRuntimeClasspathEntry[all.size()]);
//		return super.resolveClasspath(entries, configuration);
	}
	
	private void addStandardClasspathEntries(Set<IRuntimeClasspathEntry> all,
			IRuntimeClasspathEntry entry, ILaunchConfiguration configuration)
			throws CoreException {
		IRuntimeClasspathEntry[] resolved = JavaRuntime
				.resolveRuntimeClasspathEntry(entry, configuration);
		for (int j = 0; j < resolved.length; j++) {
			all.add(resolved[j]);
		}
	}

	protected void addProjectEntries(Set<IRuntimeClasspathEntry> resolved,
			IRuntimeClasspathEntry runtimeEntry, ILaunchConfiguration launchConfiguration) throws CoreException {
		IPath path = runtimeEntry.getPath();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(path.segment(0));

		IJavaProject javaProject = JavaCore.create(project);
		GradleProject gradleProject = GradleCore.create(project);
		gradleProject.getGradleModel(new NullProgressMonitor());

		boolean isMainLaunch = isMainLaunch(launchConfiguration);

		for (IClasspathEntry entry : javaProject.getRawClasspath()) {
			IRuntimeClasspathEntry rce = null;
			switch (entry.getEntryKind()) {
				case IClasspathEntry.CPE_SOURCE :
					IPath outputLocation = entry.getOutputLocation() == null
							? javaProject.getOutputLocation() : entry
									.getOutputLocation();
					if (isMainLaunch) {
						if (!isTestEntry(entry.getPath().removeFirstSegments(1))) {
							resolved.add(JavaRuntime.newArchiveRuntimeClasspathEntry(outputLocation));
						}
					} else {
						resolved.add(JavaRuntime.newArchiveRuntimeClasspathEntry(outputLocation));
					}
					break;
				case IClasspathEntry.CPE_CONTAINER :
					IClasspathContainer container = JavaCore
							.getClasspathContainer(entry.getPath(), javaProject);
					if (container != null) {
						switch (container.getKind()) {
							case IClasspathContainer.K_APPLICATION :
								rce = JavaRuntime
										.newRuntimeContainerClasspathEntry(
												container.getPath(),
												IRuntimeClasspathEntry.USER_CLASSES,
												javaProject);
								break;
						// case IClasspathContainer.K_DEFAULT_SYSTEM:
						// unresolved.add(JavaRuntime.newRuntimeContainerClasspathEntry(container.getPath(),
						// IRuntimeClasspathEntry.STANDARD_CLASSES,
						// javaProject));
						// break;
						// case IClasspathContainer.K_SYSTEM:
						// unresolved.add(JavaRuntime.newRuntimeContainerClasspathEntry(container.getPath(),
						// IRuntimeClasspathEntry.BOOTSTRAP_CLASSES,
						// javaProject));
						// break;
						}
					}
					break;
				case IClasspathEntry.CPE_LIBRARY :
					rce = JavaRuntime.newArchiveRuntimeClasspathEntry(entry
							.getPath());
					break;
				case IClasspathEntry.CPE_VARIABLE :
					if (!JavaRuntime.JRELIB_VARIABLE.equals(entry.getPath()
							.segment(0))) {
						rce = JavaRuntime
								.newVariableRuntimeClasspathEntry(entry
										.getPath());
					}
					break;
				case IClasspathEntry.CPE_PROJECT :
					IProject res = root.getProject(entry.getPath().segment(0));
					if (res != null) {
						IJavaProject otherProject = JavaCore.create(res);
						if (otherProject != null) {
							rce = JavaRuntime
									.newDefaultProjectClasspathEntry(otherProject);
						}
					}
					break;
				default :
					break;
			}
			if (rce != null) {
				addStandardClasspathEntries(resolved, rce, launchConfiguration);
			}
		}
	}

//	protected void addProjectEntries(Set<IRuntimeClasspathEntry> resolved,
//			IRuntimeClasspathEntry entry, ILaunchConfiguration launchConfiguration)
//			throws CoreException {
//		HashSet<IRuntimeClasspathEntry> added = new HashSet<IRuntimeClasspathEntry>();
//		addStandardClasspathEntries(added, entry, launchConfiguration);
//		if (isMainLaunch(launchConfiguration)) {
//			/*
//			 * Remove classpath entries applicable to tests only
//			 */
//			List<IRuntimeClasspathEntry> toRemove = new LinkedList<IRuntimeClasspathEntry>();
//			for (IRuntimeClasspathEntry classpathEntry : added) {
//				if (isTestEntry(classpathEntry.getPath())) {
//					toRemove.add(classpathEntry);
//				}
//			}
//			added.removeAll(toRemove);
//		}
//		resolved.addAll(added);
//	}
	
	protected boolean isMainLaunch(ILaunchConfiguration configuration)
			throws CoreException {
		String typeid = configuration.getType().getAttribute("id"); //$NON-NLS-1$
		if (IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION
				.equals(typeid)) {
			IResource[] resources = configuration.getMappedResources();

			if (resources == null || resources.length == 0) {
				return true;
			}

			for (int i = 0; i < resources.length; i++) {
					if (isTestEntry((resources[i]
							.getProjectRelativePath()))) {
						return false;
					}
			}
			return true;
		} else if (JUnitLaunchConfigurationConstants.ID_JUNIT_APPLICATION
				.equals(typeid)) {
			return true;
		} else {
			throw new CoreException(new Status(IStatus.ERROR,
					GradleCore.PLUGIN_ID, 0, "Unsupported launch type", null));
		}
	}
	  
	private boolean isTestEntry(IPath entryPath) {
		return TEST_SOURCE_PATH.isPrefixOf(entryPath) || TEST_RESOURCES_PATH.isPrefixOf(entryPath);
	}

}
