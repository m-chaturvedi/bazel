// Copyright 2018 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.devtools.build.lib.buildeventstream;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.devtools.build.lib.buildeventstream.BuildEvent.LocalFile;
import com.google.devtools.build.lib.vfs.Path;
import com.google.devtools.common.options.OptionsParser;
import com.google.devtools.common.options.OptionsProvider;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link BuildEventArtifactUploaderFactoryMap}. */
@RunWith(JUnit4.class)
public final class BuildEventArtifactUploaderFactoryMapTest {
  private BuildEventArtifactUploaderFactoryMap uploaderFactories;
  private BuildEventArtifactUploaderFactory noConversionUploaderFactory;
  private final OptionsParser optionsParser = OptionsParser.newOptionsParser(ImmutableList.of());

  @Before
  public void setUp() {
    noConversionUploaderFactory =
        (OptionsProvider options) ->
            new BuildEventArtifactUploader() {
              @Override
              public ListenableFuture<PathConverter> upload(Map<Path, LocalFile> files) {
                return Futures.immediateFuture(PathConverter.NO_CONVERSION);
              }

              @Override
              public void shutdown() {
                // Intentionally left empty.
              }
            };
    uploaderFactories =
        new BuildEventArtifactUploaderFactoryMap.Builder()
            .add("a", BuildEventArtifactUploaderFactory.LOCAL_FILES_UPLOADER_FACTORY)
            .add("b", noConversionUploaderFactory)
            .build();
  }

  @Test
  public void testEmptyUploaders() throws Exception {
    BuildEventArtifactUploaderFactoryMap emptyUploader =
        new BuildEventArtifactUploaderFactoryMap.Builder().build();
    assertThat(emptyUploader.select(null).create(optionsParser))
        .isEqualTo(BuildEventArtifactUploader.LOCAL_FILES_UPLOADER);
  }

  @Test
  public void testAlphabeticalOrder() {
    assertThat(uploaderFactories.select(null).create(optionsParser))
        .isEqualTo(BuildEventArtifactUploader.LOCAL_FILES_UPLOADER);
  }

  @Test
  public void testSelectByName() throws Exception {
    assertThat(uploaderFactories.select("b"))
        .isEqualTo(noConversionUploaderFactory);
  }
}
