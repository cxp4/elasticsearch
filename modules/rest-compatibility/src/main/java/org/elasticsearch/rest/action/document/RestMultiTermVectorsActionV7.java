/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.rest.action.document;

import org.elasticsearch.Version;
import org.elasticsearch.action.termvectors.MultiTermVectorsRequest;
import org.elasticsearch.action.termvectors.TermVectorsRequest;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.logging.DeprecationLogger;
import org.elasticsearch.compat.TypeConsumer;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestToXContentListener;

import java.io.IOException;
import java.util.List;

import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestRequest.Method.POST;

public class RestMultiTermVectorsActionV7 extends RestMultiTermVectorsAction {
    private static final DeprecationLogger deprecationLogger = DeprecationLogger.getLogger(RestMultiTermVectorsActionV7.class);
    static final String TYPES_DEPRECATION_MESSAGE = "[types removal] " + "Specifying types in multi term vector requests is deprecated.";

    @Override
    public List<Route> routes() {
        return List.of(
            new Route(GET, "/_mtermvectors"),
            new Route(POST, "/_mtermvectors"),
            new Route(GET, "/{index}/_mtermvectors"),
            new Route(POST, "/{index}/_mtermvectors"),
            new Route(GET, "/{index}/{type}/_mtermvectors"),
            new Route(POST, "/{index}/{type}/_mtermvectors")
        );
    }

    @Override
    public String getName() {
        return super.getName() + "_v7";
    }

    @Override
    public Version compatibleWithVersion() {
        return Version.V_7_0_0;
    }

    @Override
    public RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) throws IOException {
        TypeConsumer typeConsumer = new TypeConsumer(request, "_type");

        MultiTermVectorsRequest multiTermVectorsRequest = new MultiTermVectorsRequest();
        TermVectorsRequest template = new TermVectorsRequest().index(request.param("index"));

        RestTermVectorsAction.readURIParameters(template, request);
        multiTermVectorsRequest.ids(Strings.commaDelimitedListToStringArray(request.param("ids")));
        request.withContentOrSourceParamParserOrNull(p -> multiTermVectorsRequest.add(template, p, typeConsumer));

        if (typeConsumer.hasTypes()) {
            request.param("type");
            deprecationLogger.deprecate("termvectors_with_types", TYPES_DEPRECATION_MESSAGE);
        }
        return channel -> client.multiTermVectors(multiTermVectorsRequest, new RestToXContentListener<>(channel));
    }
}