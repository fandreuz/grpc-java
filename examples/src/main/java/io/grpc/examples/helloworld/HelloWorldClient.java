/*
 * Copyright 2015 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.grpc.examples.helloworld;

import io.grpc.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.opentelemetry.proto.collector.profiles.v1development.ExportProfilesServiceRequest;
import io.opentelemetry.proto.collector.profiles.v1development.ExportProfilesServiceResponse;
import io.opentelemetry.proto.collector.profiles.v1development.ProfilesServiceGrpc;
import io.opentelemetry.proto.profiles.v1development.Profile;
import io.opentelemetry.proto.profiles.v1development.ResourceProfiles;
import io.opentelemetry.proto.profiles.v1development.ScopeProfiles;

/**
 * A simple client that requests a greeting from the {@link HelloWorldServer}.
 */
public class HelloWorldClient {
  private static final Logger logger = Logger.getLogger(HelloWorldClient.class.getName());

  private final ProfilesServiceGrpc.ProfilesServiceBlockingStub blockingStub;
  private final DeserializeProto deserializer = new DeserializeProto();

  /** Construct client for accessing HelloWorld server using the existing channel. */
  public HelloWorldClient(Channel channel) {
    // 'channel' here is a Channel, not a ManagedChannel, so it is not this code's responsibility to
    // shut it down.

    // Passing Channels to code makes code easier to test and makes it easier to reuse Channels.
    blockingStub = ProfilesServiceGrpc.newBlockingStub(channel);
  }

  /** Say hello to server. */
  public void export(ExportProfilesServiceRequest exportProfilesServiceRequest) {
    ExportProfilesServiceResponse response;
    try {
      response = blockingStub.export(exportProfilesServiceRequest);
    } catch (StatusRuntimeException e) {
      logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
      return;
    }
    logger.info("Greeting: " + response.toString());
  }

  /**
   * Greet server. If provided, the first element of {@code args} is the name to use in the
   * greeting. The second argument is the target server.
   */
  public static void main(String[] args) throws Exception {
    // Access a service running on the local machine on port 50051
    String target = "localhost:50051";
    String path = "./ciao"
    // Allow passing in the user and target strings as command line arguments
    if (args.length <= 1) {
      System.err.println("Usage: [target][path]");
      System.err.println("");
      System.err.println("  target  The server to connect to. Defaults to " + target);
      System.err.println("  path  The path to the proto file. Defaults to " + path);
      System.exit(1);
    }
    target = args[0];
    path = args[1];

    // Create a communication channel to the server, known as a Channel. Channels are thread-safe
    // and reusable. It is common to create channels at the beginning of your application and reuse
    // them until the application shuts down.
    //
    // For the example we use plaintext insecure credentials to avoid needing TLS certificates. To
    // use TLS, use TlsChannelCredentials instead.
    ManagedChannel channel = Grpc.newChannelBuilder(target, InsecureChannelCredentials.create())
        .build();
    try {
      byte[] data = Files.readAllBytes(Path.of(path));
      HelloWorldClient client = new HelloWorldClient(channel);
      Profile profile = client.deserializer.deserializeProto(data);
      client.export(mapToExportProfilesServiceRequest(profile));
    } finally {
      // ManagedChannels use resources like threads and TCP connections. To prevent leaking these
      // resources the channel should be shut down when it will no longer be used. If it may be used
      // again leave it running.
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

  }

  private static ExportProfilesServiceRequest mapToExportProfilesServiceRequest(Profile profile) {
  ScopeProfiles scopeProfiles = ScopeProfiles.newBuilder()
          .addProfiles(profile)
          .build();
  ResourceProfiles resourceProfiles = ResourceProfiles.newBuilder()
          .addScopeProfiles(scopeProfiles)
          .build();
   return ExportProfilesServiceRequest.newBuilder()
           .addResourceProfiles(resourceProfiles)
           .build();
  }
}
