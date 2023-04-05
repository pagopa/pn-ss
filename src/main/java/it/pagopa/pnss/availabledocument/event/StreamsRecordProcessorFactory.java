/*package it.pagopa.pnss.availabledocument.event;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessorFactory;

public class StreamsRecordProcessorFactory implements IRecordProcessorFactory {
    private String disponibilitaDOcumentiEventBridge;

    public StreamsRecordProcessorFactory(String disponibilitaDOcumentiEventBridge) {
        this.disponibilitaDOcumentiEventBridge = disponibilitaDOcumentiEventBridge;
    }

    @Override
    public IRecordProcessor createProcessor() {
        return new StreamsRecordProcessor( disponibilitaDOcumentiEventBridge);
    }
}*/
