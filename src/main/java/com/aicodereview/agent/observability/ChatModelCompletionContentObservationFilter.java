package com.aicodereview.agent.observability;

import java.util.List;

import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.content.Content;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationFilter;

@Component
public class ChatModelCompletionContentObservationFilter
        implements ObservationFilter {

    @Override
    public Observation.Context map(
            Observation.Context context) {

        if (!(context instanceof ChatModelObservationContext chatContext)) {
            return context;
        }

        List<String> prompts =
                CollectionUtils.isEmpty(
                        chatContext.getRequest().getInstructions())
                        ? List.of()
                        : chatContext.getRequest()
                                .getInstructions()
                                .stream()
                                .map(Content::getText)
                                .toList();

        List<String> completions =
                processCompletion(chatContext);

        if (!prompts.isEmpty()) {

            context.addHighCardinalityKeyValue(
                    KeyValue.of(
                            "langfuse.observation.input",
                            String.join("\n", prompts)));
        }

        if (!completions.isEmpty()) {

            context.addHighCardinalityKeyValue(
                    KeyValue.of(
                            "langfuse.observation.output",
                            String.join("\n", completions)));
        }

        return context;
    }

    private List<String> processCompletion(
            ChatModelObservationContext context) {

        if (context.getResponse() == null
                || context.getResponse().getResults() == null
                || CollectionUtils.isEmpty(
                        context.getResponse().getResults())) {

            return List.of();
        }

        return context.getResponse()
                .getResults()
                .stream()
                .filter(generation ->
                        generation.getOutput() != null
                                && StringUtils.hasText(
                                        generation.getOutput()
                                                .getText()))
                .map(generation ->
                        generation.getOutput()
                                .getText())
                .toList();
    }
}