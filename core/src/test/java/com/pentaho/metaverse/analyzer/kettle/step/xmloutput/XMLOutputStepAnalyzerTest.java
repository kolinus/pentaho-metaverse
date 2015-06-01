/*
 * PENTAHO CORPORATION PROPRIETARY AND CONFIDENTIAL
 *
 * Copyright 2002 - 2015 Pentaho Corporation (Pentaho). All rights reserved.
 *
 * NOTICE: All information including source code contained herein is, and
 * remains the sole property of Pentaho and its licensors. The intellectual
 * and technical concepts contained herein are proprietary and confidential
 * to, and are trade secrets of Pentaho and may be covered by U.S. and foreign
 * patents, or patents in process, and are protected by trade secret and
 * copyright laws. The receipt or possession of this source code and/or related
 * information does not convey or imply any rights to reproduce, disclose or
 * distribute its contents, or to manufacture, use, or sell anything that it
 * may describe, in whole or in part. Any reproduction, modification, distribution,
 * or public display of this information without the express written authorization
 * from Pentaho is strictly prohibited and in violation of applicable laws and
 * international treaties. Access to the source code contained herein is strictly
 * prohibited to anyone except those individuals and entities who have executed
 * confidentiality and non-disclosure agreements or other agreements with Pentaho,
 * explicitly covering such access.
 */

package com.pentaho.metaverse.analyzer.kettle.step.xmloutput;

import com.pentaho.dictionary.DictionaryConst;
import com.pentaho.metaverse.api.IComponentDescriptor;
import com.pentaho.metaverse.api.IMetaverseBuilder;
import com.pentaho.metaverse.api.IMetaverseNode;
import com.pentaho.metaverse.api.IMetaverseObjectFactory;
import com.pentaho.metaverse.api.INamespace;
import com.pentaho.metaverse.api.MetaverseAnalyzerException;
import com.pentaho.metaverse.api.MetaverseComponentDescriptor;
import com.pentaho.metaverse.api.model.IExternalResourceInfo;
import com.pentaho.metaverse.testutils.MetaverseTestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.pentaho.di.core.ProgressMonitorListener;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaString;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.steps.xmloutput.XMLField;
import org.pentaho.di.trans.steps.xmloutput.XMLOutput;
import org.pentaho.di.trans.steps.xmloutput.XMLOutputData;
import org.pentaho.di.trans.steps.xmloutput.XMLOutputMeta;

import java.util.Collection;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;

@RunWith( MockitoJUnitRunner.class )
public class XMLOutputStepAnalyzerTest {

  private XMLOutputStepAnalyzer analyzer;

  @Mock
  private XMLOutput mockXMLOutput;

  @Mock
  private XMLOutputMeta meta;

  @Mock
  private XMLOutputData data;

  @Mock
  IMetaverseNode node;
  @Mock
  IMetaverseBuilder mockBuilder;
  @Mock
  private INamespace mockNamespace;
  @Mock
  private StepMeta parentStepMeta;
  @Mock
  private TransMeta mockTransMeta;

  IComponentDescriptor descriptor;


  @Before
  public void setUp() throws Exception {
    analyzer = spy( new XMLOutputStepAnalyzer() );
    analyzer.setMetaverseBuilder( mockBuilder );
    analyzer.setBaseStepMeta( meta );
    analyzer.setRootNode( node );
    analyzer.setParentTransMeta( mockTransMeta );
    analyzer.setParentStepMeta( parentStepMeta );

    descriptor = new MetaverseComponentDescriptor( "test", DictionaryConst.NODE_TYPE_TRANS_STEP, mockNamespace );
    analyzer.setDescriptor( descriptor );
    analyzer.setObjectFactory( MetaverseTestUtils.getMetaverseObjectFactory() );

    when( mockXMLOutput.getStepDataInterface() ).thenReturn( data );
    when( mockXMLOutput.getStepMeta() ).thenReturn( parentStepMeta );

    when( meta.getParentStepMeta() ).thenReturn( parentStepMeta );
    when( parentStepMeta.getStepMetaInterface() ).thenReturn( meta );
    when( parentStepMeta.getParentTransMeta() ).thenReturn( mockTransMeta );
    when( parentStepMeta.getName() ).thenReturn( "test" );
    when( parentStepMeta.getStepID() ).thenReturn( "XmlOutputStep" );
  }

  @Test
  public void testCustomAnalyze() throws Exception {
    when( meta.getMainElement() ).thenReturn( "main" );
    when( meta.getRepeatElement() ).thenReturn( "repeat" );
    analyzer.customAnalyze( meta, node );

    verify( node ).setProperty( "parentnode", "main" );
    verify( node ).setProperty( "rownode", "repeat" );

  }

  @Test
  public void testGetSupportedSteps() {
    XMLOutputStepAnalyzer analyzer = new XMLOutputStepAnalyzer();
    Set<Class<? extends BaseStepMeta>> types = analyzer.getSupportedSteps();
    assertNotNull( types );
    assertEquals( types.size(), 1 );
    assertTrue( types.contains( XMLOutputMeta.class ) );
  }

  @Test
  public void testGetOutputResourceFields() throws Exception {
    XMLField[] outputFields = new XMLField[2];
    XMLField field1 = mock( XMLField.class );
    XMLField field2 = mock( XMLField.class );
    outputFields[0] = field1;
    outputFields[1] = field2;

    when( field1.getFieldName() ).thenReturn( "field1" );
    when( field2.getFieldName() ).thenReturn( "field2" );

    when( meta.getOutputFields() ).thenReturn( outputFields );

    Set<String> outputResourceFields = analyzer.getOutputResourceFields( meta );

    assertEquals( outputFields.length, outputResourceFields.size() );
    for ( XMLField outputField : outputFields ) {
      assertTrue( outputResourceFields.contains( outputField.getFieldName() ) );
    }
  }

  @Test
  public void testXMLOutputExternalResourceConsumer() throws Exception {
    XMLOutputExternalResourceConsumer consumer = new XMLOutputExternalResourceConsumer();

    StepMeta meta = new StepMeta( "test", this.meta );
    StepMeta spyMeta = spy( meta );

    when( this.meta.getParentStepMeta() ).thenReturn( spyMeta );
    when( spyMeta.getParentTransMeta() ).thenReturn( mockTransMeta );
    when( this.meta.getFileName() ).thenReturn( null );
    String[] filePaths = { "/path/to/file1", "/another/path/to/file2" };
    when( this.meta.getFiles( Mockito.any( VariableSpace.class ) ) ).thenReturn( filePaths );

    assertFalse( consumer.isDataDriven( this.meta ) );
    Collection<IExternalResourceInfo> resources = consumer.getResourcesFromMeta( this.meta );
    assertFalse( resources.isEmpty() );
    assertEquals( 2, resources.size() );

    when( this.meta.getExtension() ).thenReturn( "txt" );

    assertEquals( XMLOutputMeta.class, consumer.getMetaClass() );
  }

  @Test
  public void testCreateResourceNode() throws Exception {
    IExternalResourceInfo res = mock( IExternalResourceInfo.class );
    when( res.getName() ).thenReturn( "file:///Users/home/tmp/xyz.xml" );
    IMetaverseNode resourceNode = analyzer.createResourceNode( res );
    assertNotNull( resourceNode );
    assertEquals( DictionaryConst.NODE_TYPE_FILE, resourceNode.getType() );
  }
  @Test
  public void testGetResourceInputNodeType() throws Exception {
    assertNull( analyzer.getResourceInputNodeType() );
  }

  @Test
  public void testGetResourceOutputNodeType() throws Exception {
    assertEquals( DictionaryConst.NODE_TYPE_FILE_FIELD, analyzer.getResourceOutputNodeType() );
  }

  @Test
  public void testIsOutput() throws Exception {
    assertTrue( analyzer.isOutput() );
  }

  @Test
  public void testIsInput() throws Exception {
    assertFalse( analyzer.isInput() );
  }

  @Test
  public void testGetUsedFields() throws Exception {
    assertNull( analyzer.getUsedFields( meta ) );
  }
}

