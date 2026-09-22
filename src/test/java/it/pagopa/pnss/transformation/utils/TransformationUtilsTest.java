package it.pagopa.pnss.transformation.utils;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.model.Tag;

import java.util.List;
import java.util.Optional;

import static it.pagopa.pnss.configurationproperties.TransformationProperties.ERROR;
import static it.pagopa.pnss.configurationproperties.TransformationProperties.OK;
import static it.pagopa.pnss.configurationproperties.TransformationProperties.TRANSFORMATION_TAG_PREFIX;
import static it.pagopa.pnss.transformation.utils.TransformationUtils.TRANSFORMATION_IN_PROGRESS;
import static it.pagopa.pnss.transformation.utils.TransformationUtils.selectTransformationTag;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransformationUtilsTest {

    private static final String DUMMY = "DUMMY";
    private static final String RASTER = "RASTER";
    private static final String SIGN_AND_TIMEMARK = "SIGN_AND_TIMEMARK";
    private static final List<String> CHAIN = List.of(DUMMY, RASTER, SIGN_AND_TIMEMARK);

    private Tag tag(String transformation, String value) {
        return Tag.builder().key(TRANSFORMATION_TAG_PREFIX + transformation).value(value).build();
    }

    @Test
    void selectTransformationTag_EmptyTagSet_Empty() {
        assertTrue(selectTransformationTag(List.of(), CHAIN).isEmpty());
    }

    @Test
    void selectTransformationTag_NoTransformationTag_Empty() {
        assertTrue(selectTransformationTag(List.of(Tag.builder().key("Other").value(OK).build()), CHAIN).isEmpty());
    }

    @Test
    void selectTransformationTag_InProgressTagNotFirstInAlphabeticalOrder_ReturnsInProgressTag() {
        List<Tag> tagSet = List.of(tag(DUMMY, OK), tag(RASTER, TRANSFORMATION_IN_PROGRESS));

        Optional<Tag> selected = selectTransformationTag(tagSet, CHAIN);

        assertEquals(tag(RASTER, TRANSFORMATION_IN_PROGRESS), selected.orElseThrow());
    }

    @Test
    void selectTransformationTag_ErrorTagWithCompletedTags_ReturnsErrorTag() {
        List<Tag> tagSet = List.of(tag(DUMMY, OK), tag(RASTER, ERROR));

        Optional<Tag> selected = selectTransformationTag(tagSet, CHAIN);

        assertEquals(tag(RASTER, ERROR), selected.orElseThrow());
    }

    @Test
    void selectTransformationTag_ErrorPrecedingInProgress_ReturnsMostAdvancedTag() {
        List<Tag> tagSet = List.of(tag(DUMMY, ERROR), tag(RASTER, TRANSFORMATION_IN_PROGRESS));

        Optional<Tag> selected = selectTransformationTag(tagSet, CHAIN);

        assertEquals(tag(RASTER, TRANSFORMATION_IN_PROGRESS), selected.orElseThrow());
    }

    @Test
    void selectTransformationTag_CompletedTagsOnly_ReturnsLastInChainOrder() {
        List<Tag> tagSet = List.of(tag(DUMMY, OK), tag(RASTER, OK));

        Optional<Tag> selected = selectTransformationTag(tagSet, CHAIN);

        assertEquals(tag(RASTER, OK), selected.orElseThrow());
    }

    @Test
    void selectTransformationTag_MultipleInProgressTags_ReturnsLastInChainOrder() {
        List<Tag> tagSet = List.of(tag(RASTER, TRANSFORMATION_IN_PROGRESS), tag(SIGN_AND_TIMEMARK, TRANSFORMATION_IN_PROGRESS));

        Optional<Tag> selected = selectTransformationTag(tagSet, CHAIN);

        assertEquals(tag(SIGN_AND_TIMEMARK, TRANSFORMATION_IN_PROGRESS), selected.orElseThrow());
    }

    @Test
    void selectTransformationTag_TransformationNotInChain_ReturnsTag() {
        List<Tag> tagSet = List.of(tag("NONE", OK));

        Optional<Tag> selected = selectTransformationTag(tagSet, CHAIN);

        assertEquals(tag("NONE", OK), selected.orElseThrow());
    }
}
