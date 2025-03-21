package io.grpc.examples.helloworld;

import com.google.protobuf.InvalidProtocolBufferException;
import io.opentelemetry.proto.profiles.v1development.Profile;

public class DeserializeProto {

    Profile deserializeProto(byte[] data) throws InvalidProtocolBufferException {
        return Profile.parseFrom(data);
    }
}
