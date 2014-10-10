package org.springsource.ide.eclipse.gradle.core.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationListener;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaRuntime;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleNature;

public class GradleLaunchConfigListener implements ILaunchConfigurationListener {

	@Override
	public void launchConfigurationAdded(ILaunchConfiguration configuration) {
		updateLaunchConfiguration(configuration);
	}

	@Override
	public void launchConfigurationChanged(ILaunchConfiguration configuration) {
		updateLaunchConfiguration(configuration);
	}

	@Override
	public void launchConfigurationRemoved(ILaunchConfiguration configuration) {
		// nothing
	}

	private void updateLaunchConfiguration(ILaunchConfiguration configuration) {
		try {
			if (!GradleRuntimeClasspathProvider.isSupportedType(configuration
					.getType().getIdentifier())) {
				return;
			}
			if (configuration.getAttributes().containsKey(
					IJavaLaunchConfigurationConstants.ATTR_CLASSPATH_PROVIDER)) {
				return;
			}
			IJavaProject javaProject = JavaRuntime
					.getJavaProject(configuration);
			if (javaProject != null
					&& GradleNature.hasNature(javaProject.getProject())) {
				GradleRuntimeClasspathProvider.enable(configuration);
			}
		} catch (CoreException ex) {
			GradleCore.log(ex);
		}
	}
}
