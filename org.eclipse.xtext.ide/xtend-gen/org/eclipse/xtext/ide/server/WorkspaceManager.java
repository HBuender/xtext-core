/**
 * Copyright (c) 2016 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.xtext.ide.server;

import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.xtext.ide.server.BuildManager;
import org.eclipse.xtext.ide.server.Document;
import org.eclipse.xtext.ide.server.ILanguageServerAccess;
import org.eclipse.xtext.ide.server.IProjectDescriptionFactory;
import org.eclipse.xtext.ide.server.IWorkspaceConfigFactory;
import org.eclipse.xtext.ide.server.ProjectManager;
import org.eclipse.xtext.resource.IExternalContentSupport;
import org.eclipse.xtext.resource.IResourceDescription;
import org.eclipse.xtext.resource.IResourceDescriptions;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.resource.impl.ChunkedResourceDescriptions;
import org.eclipse.xtext.resource.impl.ProjectDescription;
import org.eclipse.xtext.resource.impl.ResourceDescriptionsData;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.util.internal.Log;
import org.eclipse.xtext.validation.Issue;
import org.eclipse.xtext.workspace.IProjectConfig;
import org.eclipse.xtext.workspace.IWorkspaceConfig;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Functions.Function2;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure2;

/**
 * @author Sven Efftinge - Initial contribution and API
 * @since 2.11
 */
@Log
@SuppressWarnings("all")
public class WorkspaceManager {
  @Inject
  private Provider<ProjectManager> projectManagerProvider;
  
  @Inject
  private IWorkspaceConfigFactory workspaceConfigFactory;
  
  @Inject
  private IProjectDescriptionFactory projectDescriptionFactory;
  
  private BuildManager buildManager;
  
  private Map<String, ProjectManager> projectName2ProjectManager = CollectionLiterals.<String, ProjectManager>newHashMap();
  
  private URI baseDir;
  
  private Procedure2<? super URI, ? super Iterable<Issue>> issueAcceptor;
  
  private IWorkspaceConfig workspaceConfig;
  
  private List<ILanguageServerAccess.IBuildListener> buildListeners = CollectionLiterals.<ILanguageServerAccess.IBuildListener>newArrayList();
  
  public void addBuildListener(final ILanguageServerAccess.IBuildListener listener) {
    this.buildListeners.add(listener);
  }
  
  private Map<String, ResourceDescriptionsData> fullIndex = CollectionLiterals.<String, ResourceDescriptionsData>newHashMap();
  
  private Map<URI, Document> openDocuments = CollectionLiterals.<URI, Document>newHashMap();
  
  private final IExternalContentSupport.IExternalContentProvider openedDocumentsContentProvider = new IExternalContentSupport.IExternalContentProvider() {
    @Override
    public IExternalContentSupport.IExternalContentProvider getActualContentProvider() {
      return this;
    }
    
    @Override
    public String getContent(final URI uri) {
      Document _get = WorkspaceManager.this.openDocuments.get(uri);
      String _contents = null;
      if (_get!=null) {
        _contents=_get.getContents();
      }
      return _contents;
    }
    
    @Override
    public boolean hasContent(final URI uri) {
      return WorkspaceManager.this.openDocuments.containsKey(uri);
    }
  };
  
  @Inject
  public void setBuildManager(final BuildManager buildManager) {
    buildManager.setWorkspaceManager(this);
    this.buildManager = buildManager;
  }
  
  public void initialize(final URI baseDir, final Procedure2<? super URI, ? super Iterable<Issue>> issueAcceptor, final CancelIndicator cancelIndicator) {
    this.baseDir = baseDir;
    this.issueAcceptor = issueAcceptor;
    this.refreshWorkspaceConfig(cancelIndicator);
  }
  
  protected void refreshWorkspaceConfig(final CancelIndicator cancelIndicator) {
    this.workspaceConfig = this.workspaceConfigFactory.getWorkspaceConfig(this.baseDir);
    final ArrayList<ProjectDescription> newProjects = CollectionLiterals.<ProjectDescription>newArrayList();
    final HashSet<Set<String>> remainingProjectNames = CollectionLiterals.<Set<String>>newHashSet(this.projectName2ProjectManager.keySet());
    final Consumer<IProjectConfig> _function = (IProjectConfig projectConfig) -> {
      boolean _containsKey = this.projectName2ProjectManager.containsKey(projectConfig.getName());
      if (_containsKey) {
        remainingProjectNames.remove(projectConfig.getName());
      } else {
        final ProjectManager projectManager = this.projectManagerProvider.get();
        final ProjectDescription projectDescription = this.projectDescriptionFactory.getProjectDescription(projectConfig);
        final Provider<Map<String, ResourceDescriptionsData>> _function_1 = () -> {
          return this.fullIndex;
        };
        projectManager.initialize(projectDescription, projectConfig, this.issueAcceptor, this.openedDocumentsContentProvider, _function_1, cancelIndicator);
        this.projectName2ProjectManager.put(projectDescription.getName(), projectManager);
        newProjects.add(projectDescription);
      }
    };
    this.workspaceConfig.getProjects().forEach(_function);
    for (final Set<String> deletedProject : remainingProjectNames) {
      {
        this.projectName2ProjectManager.remove(deletedProject);
        this.fullIndex.remove(deletedProject);
      }
    }
    final List<IResourceDescription.Delta> result = this.buildManager.doInitialBuild(newProjects, cancelIndicator);
    this.afterBuild(result);
  }
  
  protected void afterBuild(final List<IResourceDescription.Delta> deltas) {
    for (final ILanguageServerAccess.IBuildListener listener : this.buildListeners) {
      listener.afterBuild(deltas);
    }
  }
  
  public List<IResourceDescription.Delta> doBuild(final List<URI> dirtyFiles, final List<URI> deletedFiles, final CancelIndicator cancelIndicator) {
    final List<IResourceDescription.Delta> doBuild = this.buildManager.doBuild(dirtyFiles, deletedFiles, cancelIndicator);
    this.afterBuild(doBuild);
    return doBuild;
  }
  
  public IResourceDescriptions getIndex() {
    return new ChunkedResourceDescriptions(this.fullIndex);
  }
  
  public URI getProjectBaseDir(final URI candidate) {
    URI _xblockexpression = null;
    {
      final IProjectConfig projectConfig = this.workspaceConfig.findProjectContaining(candidate);
      URI _path = null;
      if (projectConfig!=null) {
        _path=projectConfig.getPath();
      }
      _xblockexpression = _path;
    }
    return _xblockexpression;
  }
  
  public ProjectManager getProjectManager(final URI uri) {
    final IProjectConfig projectConfig = this.workspaceConfig.findProjectContaining(uri);
    String _name = null;
    if (projectConfig!=null) {
      _name=projectConfig.getName();
    }
    return this.projectName2ProjectManager.get(_name);
  }
  
  public ProjectManager getProjectManager(final String projectName) {
    return this.projectName2ProjectManager.get(projectName);
  }
  
  public List<ProjectManager> getProjectManagers() {
    Collection<ProjectManager> _values = this.projectName2ProjectManager.values();
    return new ArrayList<ProjectManager>(_values);
  }
  
  public void didChange(final URI uri, final int version, final Iterable<TextEdit> changes, final CancelIndicator cancelIndicator) {
    boolean _containsKey = this.openDocuments.containsKey(uri);
    boolean _not = (!_containsKey);
    if (_not) {
      WorkspaceManager.LOG.error((("The document " + uri) + " has not been opened."));
      return;
    }
    final Document contents = this.openDocuments.get(uri);
    this.openDocuments.put(uri, contents.applyChanges(changes));
    this.doBuild(Collections.<URI>unmodifiableList(CollectionLiterals.<URI>newArrayList(uri)), CollectionLiterals.<URI>newArrayList(), cancelIndicator);
  }
  
  public List<IResourceDescription.Delta> didOpen(final URI uri, final int version, final String contents, final CancelIndicator cancelIndicator) {
    List<IResourceDescription.Delta> _xblockexpression = null;
    {
      Document _document = new Document(version, contents);
      this.openDocuments.put(uri, _document);
      _xblockexpression = this.doBuild(Collections.<URI>unmodifiableList(CollectionLiterals.<URI>newArrayList(uri)), CollectionLiterals.<URI>newArrayList(), cancelIndicator);
    }
    return _xblockexpression;
  }
  
  public List<IResourceDescription.Delta> didClose(final URI uri, final CancelIndicator cancelIndicator) {
    List<IResourceDescription.Delta> _xblockexpression = null;
    {
      this.openDocuments.remove(uri);
      List<IResourceDescription.Delta> _xifexpression = null;
      boolean _exists = this.exists(uri);
      if (_exists) {
        _xifexpression = this.doBuild(Collections.<URI>unmodifiableList(CollectionLiterals.<URI>newArrayList(uri)), CollectionLiterals.<URI>newArrayList(), cancelIndicator);
      } else {
        _xifexpression = this.doBuild(CollectionLiterals.<URI>newArrayList(), Collections.<URI>unmodifiableList(CollectionLiterals.<URI>newArrayList(uri)), cancelIndicator);
      }
      _xblockexpression = _xifexpression;
    }
    return _xblockexpression;
  }
  
  protected boolean exists(final URI uri) {
    ProjectManager _projectManager = this.getProjectManager(uri);
    XtextResourceSet _resourceSet = null;
    if (_projectManager!=null) {
      _resourceSet=_projectManager.getResourceSet();
    }
    return _resourceSet.getURIConverter().exists(uri, null);
  }
  
  public <T extends Object> T doRead(final URI uri, final Function2<? super Document, ? super XtextResource, ? extends T> work) {
    final URI resourceURI = uri.trimFragment();
    final ProjectManager projectMnr = this.getProjectManager(resourceURI);
    Resource _resource = null;
    if (projectMnr!=null) {
      _resource=projectMnr.getResource(resourceURI);
    }
    final XtextResource resource = ((XtextResource) _resource);
    if ((resource == null)) {
      return work.apply(null, null);
    }
    Document doc = this.getDocument(resource);
    Resource _resource_1 = projectMnr.getResource(resourceURI);
    return work.apply(doc, ((XtextResource) _resource_1));
  }
  
  protected Document getDocument(final XtextResource resource) {
    Document _elvis = null;
    Document _get = this.openDocuments.get(resource.getURI());
    if (_get != null) {
      _elvis = _get;
    } else {
      String _text = resource.getParseResult().getRootNode().getText();
      Document _document = new Document(1, _text);
      _elvis = _document;
    }
    return _elvis;
  }
  
  public boolean isDocumentOpen(final URI uri) {
    return this.openDocuments.containsKey(uri);
  }
  
  private final static Logger LOG = Logger.getLogger(WorkspaceManager.class);
}
