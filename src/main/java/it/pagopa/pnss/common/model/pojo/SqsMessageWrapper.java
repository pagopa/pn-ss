package it.pagopa.pnss.common.model.pojo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.services.sqs.model.Message;

@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@Getter
public class SqsMessageWrapper<T> {

    Message message;
    T messageContent;
}
