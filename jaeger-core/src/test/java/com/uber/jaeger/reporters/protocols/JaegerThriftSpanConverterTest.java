/*
 * Copyright (c) 2016, Uber Technologies, Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.uber.jaeger.reporters.protocols;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import com.uber.jaeger.Tracer;
import com.uber.jaeger.reporters.InMemoryReporter;
import com.uber.jaeger.samplers.ConstSampler;
import com.uber.jaeger.thriftjava.Log;
import com.uber.jaeger.thriftjava.Tag;
import com.uber.jaeger.thriftjava.TagType;
import io.opentracing.Span;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(DataProviderRunner.class)
public class JaegerThriftSpanConverterTest {
  Tracer tracer;

  @Before
  public void setUp() {
    tracer =
        new Tracer.Builder("test-service-name", new InMemoryReporter(), new ConstSampler(true))
            .build();
  }

  @DataProvider
  public static Object[][] dataProviderBuildTag() {
    // @formatter:off
    return new Object[][] {
        { "value", TagType.STRING, "value" },
        { new Long(1), TagType.LONG, new Long(1) },
        { new Integer(1), TagType.LONG, new Long(1) },
        { new Short((short) 1), TagType.LONG, new Long(1) },
        { new Double(1), TagType.DOUBLE, new Double(1) },
        { new Float(1), TagType.DOUBLE, new Double(1) },
        { new Byte((byte) 1), TagType.STRING, "1" },
        { true, TagType.BOOL, true },
        { new ArrayList<String>() {{add("hello");}}, TagType.STRING, "[hello]"}
    };
    // @formatter:on
  }

  @Test
  @UseDataProvider("dataProviderBuildTag")
  public void testBuildTag(Object tagValue, TagType tagType, Object expected) {
    Tag tag = JaegerThriftSpanConverter.buildTag("key", tagValue);
    assertEquals(tagType, tag.getVType());
    assertEquals("key", tag.getKey());
    switch (tagType) {
      case STRING:
        assertEquals(expected, tag.getVStr());
        break;
      case BOOL:
        assertEquals(expected, tag.isVBool());
        break;
      case LONG:
        assertEquals(expected, tag.getVLong());
        break;
      case DOUBLE:
        assertEquals(expected, tag.getVDouble());
        break;
      case BINARY:
        break;
    }
  }

  @Test
  public void testBuildTags() {
    Map<String, Object> tags = new HashMap<>();
    tags.put("key", "value");

    List<Tag> jTags = JaegerThriftSpanConverter.buildTags(tags);
    assertNotNull(jTags);
    assertEquals(1, jTags.size());
    assertEquals("key", jTags.get(0).getKey());
    assertEquals("value", jTags.get(0).getVStr());
    assertEquals(TagType.STRING, jTags.get(0).getVType());
  }

  @Test
  public void testConvertSpan() {
    Span span = tracer.buildSpan("operation-name").start();
    span = span.log(1, "key", "value");

    com.uber.jaeger.thriftjava.Span jSpan = JaegerThriftSpanConverter.convertSpan((com.uber.jaeger.Span) span);

    assertEquals("operation-name", jSpan.getOperationName());
    assertEquals(1, jSpan.getLogs().size());
    Log jLog = jSpan.getLogs().get(0);
    assertEquals(1, jLog.getTimestamp());
    assertEquals(1, jLog.getFields().size());
    Tag jTag = jLog.getFields().get(0);
    assertEquals("key", jTag.getKey());
    assertEquals("value", jTag.getVStr());
  }
}
