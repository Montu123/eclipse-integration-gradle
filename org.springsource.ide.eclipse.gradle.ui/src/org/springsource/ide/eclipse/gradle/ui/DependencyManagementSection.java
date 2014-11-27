/*******************************************************************************
 * Copyright (c) 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.ui;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.m2e.M2EUtils;
import org.springsource.ide.eclipse.gradle.core.preferences.GradlePreferences;
import org.springsource.ide.eclipse.gradle.core.util.expression.LiveExpression;
import org.springsource.ide.eclipse.gradle.core.validators.CompositeValidator;
import org.springsource.ide.eclipse.gradle.core.validators.ValidationResult;

/**
 * @author Kris De Volder
 */
public class DependencyManagementSection extends PrefsPageSection {
	
	LiveExpression<ValidationResult> validator = new CompositeValidator();
	
	Text autoRefreshDelayText;
	Button enableAutoRefreshButton;

	private Button enableJarToMvnProjectMappingButton;
	private Button enableJarToGradleProjectMappingButton;
	private Button enableRemapJarsInHierarchyButton;
	
	private Button enableJarRemappingOnOpenClose;
	
	private Button exportDependencies;
	
	public DependencyManagementSection(GradlePreferencesPage owner) {
		super(owner);
	}

	@Override
	public boolean performOK() {
		setEnableAutoRefresh(getEnableAutoRefreshInPage());
		setAutoRefreshDelay(getAutoRefreshDelayInPage());
		setExportDependencies(getExportDependenciesInPage());
		if (enableJarToMvnProjectMappingButton!=null) {
			//This can be null if M2E is not installed. In that case the option is not supported and the UI widgetry for it is not created.
			setRemapJarsToMavenProjects(enableJarToMvnProjectMappingButton.getSelection());
		}
		setRemapJarsToGradleProjects(enableJarToGradleProjectMappingButton.getSelection());
		setRemapJarsInHierarchy(enableRemapJarsInHierarchyButton.getSelection());
		setJarRemappingOnOpenClose(enableJarRemappingOnOpenClose.getSelection());
		return true;
	}

	@Override
	public void performDefaults() {
		setEnableAutoRefreshInPage(GradlePreferences.DEFAULT_AUTO_REFRESH_DEPENDENCIES);
		setAutoRefreshDelayInPage(GradlePreferences.DEFAULT_AUTO_REFRESH_DELAY);
		setExportDependenciesInPage(GradlePreferences.DEFAULT_EXPORT_DEPENDENCIES);
		
		setRemapJarsToMavenProjectsInPage(GradlePreferences.DEFAULT_JAR_REMAP_GRADLE_TO_MAVEN);
		setRemapJarsToGradleProjectsInPage(GradlePreferences.DEFAULT_JAR_REMAP_GRADLE_TO_GRADLE);
		setRemapJarsInHierarchyInPage(GradlePreferences.DEFAULT_JAR_REMAP_IN_HIERARCHY);
		
		setJarRemappingOnOpenCloseInPage(GradlePreferences.DEFAULT_JAR_REMAP_ON_OPEN_CLOSE);
	}

	@Override
	public LiveExpression<ValidationResult> getValidator() {
		return validator;
	}

	@Override
	public void createContents(Composite page) {
        GridDataFactory grabHorizontal = GridDataFactory.fillDefaults().grab(true, false);
		GridDataFactory span2 = GridDataFactory.fillDefaults().span(2, 1);
        
		Label label = new Label(page, SWT.NONE);
		label.setText("Dependency Management");

        Composite composite = new Composite(page, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        composite.setLayout(layout);
        
        //Export 'Gradle Dependencies'?
        exportDependencies = new Button(composite, SWT.CHECK);
        exportDependencies.setText("Export 'Gradle Dependencies' classpath elements");
        exportDependencies.setToolTipText("Should the entries in 'Gradle Dependencies' classpath container be exported\n" +
        		"This option only affects projects with 'Dependency Management' enabled. " +
        		"Other projects use whatever is generated by running the Gradle 'eclipse' task.");
        span2.applyTo(exportDependencies);
        setExportDependenciesInPage(getExportDependencies());
		
		//Enable auto refresh checkbox
        
        enableAutoRefreshButton = new Button(composite, SWT.CHECK);
        enableAutoRefreshButton.setText("Enable automatic refresh. Delay (ms) : ");
        enableAutoRefreshButton.setToolTipText("Automatically refresh 'Gradle Depencies' when any .gradle file is changed");
        enableAutoRefreshButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
			public void widgetSelected(SelectionEvent e) {
				enableDisableWidgets();
			}
        });

		setEnableAutoRefreshInPage(getEnableAutoRefresh());

		//Refresh delay text widget
		autoRefreshDelayText = new Text(composite, SWT.BORDER);
		autoRefreshDelayText.setToolTipText("Delay between change event and triggered auto refresh.");
		grabHorizontal.applyTo(composite);
		grabHorizontal.applyTo(autoRefreshDelayText);
		
		setAutoRefreshDelayInPage(getAutoRefreshDelay());
		
        enableJarToMvnProjectMappingButton = new Button(composite, SWT.CHECK);
        enableJarToMvnProjectMappingButton.setText("Remap Jars to maven projects (requires Gradle 1.1 and m2e)");
        enableJarToMvnProjectMappingButton.setToolTipText("Try to replace jars in Gradle Dependencies by dependencies to maven projects in the workspace.");
        enableJarToMvnProjectMappingButton.setSelection(GradleCore.getInstance().getPreferences().getRemapJarsToMavenProjects());
        enableJarToMvnProjectMappingButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
			public void widgetSelected(SelectionEvent e) {
				enableDisableWidgets();
			}
        });
        if (!M2EUtils.isInstalled()) {
        	enableJarToMvnProjectMappingButton.setEnabled(false);
        }
        span2.applyTo(enableJarToMvnProjectMappingButton);
        
        enableJarToGradleProjectMappingButton = new Button(composite, SWT.CHECK);
        enableJarToGradleProjectMappingButton.setText("Remap Jars to Gradle Projects (requires Gradle 1.12 or later)");
        enableJarToGradleProjectMappingButton.setToolTipText("Try to replace jars in Gradle Dependencies by dependencies to Gradle projects in the workspace.");
        enableJarToGradleProjectMappingButton.setSelection(GradleCore.getInstance().getPreferences().getRemapJarsToGradleProjects());
        enableJarToGradleProjectMappingButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
			public void widgetSelected(SelectionEvent e) {
				enableDisableWidgets();
			}
        });

		span2.applyTo(enableJarToGradleProjectMappingButton);
		
		enableRemapJarsInHierarchyButton = new Button(composite, SWT.CHECK);
		enableRemapJarsInHierarchyButton.setText("Jar remapping within a hierachy (requires Gradle 1.12)");
		enableRemapJarsInHierarchyButton.setToolTipText("When enabled tools will try to remap dependencyies to"
				+ " Gradle project in the same project hierarchy to an equivalent jar, if"
				+ " the corresponding project is closed or otherwise not available in the "
				+ " workspace");
		span2.applyTo(enableRemapJarsInHierarchyButton);
		
        enableJarRemappingOnOpenClose = new Button(composite, SWT.CHECK);
        enableJarRemappingOnOpenClose.setText("Jar Remapping on Project Open/Close");
        enableJarRemappingOnOpenClose.setToolTipText("When jar remapping is enabled, recompute remappings automatically"
        		+ "when projects in the workspace are openened or closed.");
        enableJarRemappingOnOpenClose.setSelection(GradleCore.getInstance().getPreferences().getJarRemappingOnOpenClose());
		span2.applyTo(enableJarRemappingOnOpenClose);
		
		enableDisableWidgets();
	}

	private void enableDisableWidgets() {
		enableDisableWidgets(enableAutoRefreshButton, autoRefreshDelayText);
		if (enableJarToGradleProjectMappingButton!=null 
				&& enableJarToMvnProjectMappingButton!=null 
				&& enableJarRemappingOnOpenClose!=null
				&& enableRemapJarsInHierarchyButton!=null) {
			boolean openCloseListerWidgetEnabled = enableJarToGradleProjectMappingButton.getSelection() 
					|| enableJarToMvnProjectMappingButton.getSelection()
					|| enableRemapJarsInHierarchyButton.getSelection();
			enableJarRemappingOnOpenClose.setEnabled(openCloseListerWidgetEnabled);
		}
	}

	public void enableDisableWidgets(Button radio, Control... others) {
		boolean enable = radio.getSelection();
		for (Control widget : others) {
			widget.setEnabled(enable);
		}
	}
	
	////////////////// 'in page' getters and setters //////////////////////////
	
	public boolean getEnableAutoRefreshInPage() {
		if (enableAutoRefreshButton!=null) {
			return enableAutoRefreshButton.getSelection();
		}
		return GradlePreferences.DEFAULT_AUTO_REFRESH_DEPENDENCIES;
	}

	public void setEnableAutoRefreshInPage(boolean e) {
		enableAutoRefreshButton.setSelection(e);
	}

	private void setAutoRefreshDelayInPage(int v) {
		autoRefreshDelayText.setText(""+v);
	}
	
	private int getAutoRefreshDelayInPage() {
		if (autoRefreshDelayText!=null) {
			try {
				return Integer.parseInt(autoRefreshDelayText.getText());
			} catch (NumberFormatException e) {
			}
		}
		return GradlePreferences.DEFAULT_AUTO_REFRESH_DELAY;
	}
	
	private boolean getExportDependenciesInPage() {
		return exportDependencies.getSelection();
	}
	
	private void setExportDependenciesInPage(boolean enable) {
		exportDependencies.setSelection(enable);
	}
	
	private void setRemapJarsToMavenProjectsInPage(boolean enable) {
		enableJarToMvnProjectMappingButton.setSelection(enable);
	}

	private void setRemapJarsToGradleProjectsInPage(boolean enable) {
		enableJarToGradleProjectMappingButton.setSelection(enable);
	}
	private void setRemapJarsInHierarchyInPage(boolean enable) {
		enableRemapJarsInHierarchyButton.setSelection(enable);
	}

	private void setJarRemappingOnOpenCloseInPage(boolean v) {
		enableJarRemappingOnOpenClose.setSelection(v);
	}
	
	///////////// preferences getters and setters /////////////////////////////////

	private boolean getEnableAutoRefresh() {
		return GradleCore.getInstance().getPreferences().isAutoRefreshDependencies();
	}
	
	private boolean getExportDependencies() {
		return GradleCore.getInstance().getPreferences().isExportDependencies();
	}

	private void setExportDependencies(boolean e) {
		GradleCore.getInstance().getPreferences().setExportDependencies(e);
	}
	
	private int getAutoRefreshDelay() {
		return GradleCore.getInstance().getPreferences().getAutoRefreshDelay();
	}
	
	private void setEnableAutoRefresh(boolean e) {
		GradleCore.getInstance().getPreferences().setAutoRefreshDependencies(e);
	}

	private void setAutoRefreshDelay(int v) {
		GradleCore.getInstance().getPreferences().setAutoRefreshDelay(v);
	}

	private void setRemapJarsToMavenProjects(boolean v) {
		GradleCore.getInstance().getPreferences().setRemapJarsToMavenProjects(v);
	}
	
	private void setRemapJarsToGradleProjects(boolean v) {
		GradleCore.getInstance().getPreferences().setRemapJarsToGradleProjects(v);
	}
	
	private void setRemapJarsInHierarchy(boolean v) {
		GradleCore.getInstance().getPreferences().setRemapJarsInHierarchy(v);
	}

	private void setJarRemappingOnOpenClose(boolean v) {
		GradleCore.getInstance().getPreferences().setJarRemappingOnOpenClose(v);
		
	}

}
