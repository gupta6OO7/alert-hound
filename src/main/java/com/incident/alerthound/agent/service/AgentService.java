package com.incident.alerthound.agent.service;

import com.incident.alerthound.agent.model.AgentActionType;
import com.incident.alerthound.agent.model.AgentContext;
import com.incident.alerthound.agent.model.AgentDecision;
import com.incident.alerthound.agent.model.AgentInvestigationReport;
import com.incident.alerthound.agent.model.AgentInvestigationStep;
import com.incident.alerthound.agent.model.AgentResultEvent;
import com.incident.alerthound.agent.model.ToolResult;
import com.incident.alerthound.config.AlertHoundProperties;
import com.incident.alerthound.incident.model.AgentTaskEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AgentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentService.class);

    private final ToolExecutor toolExecutor;
    private final RagService ragService;
    private final LlmClient llmClient;
    private final AgentResultProducer agentResultProducer;
    private final int maxIterations;

    public AgentService(
            ToolExecutor toolExecutor,
            RagService ragService,
            LlmClient llmClient,
            AgentResultProducer agentResultProducer,
            AlertHoundProperties properties
    ) {
        this.toolExecutor = toolExecutor;
        this.ragService = ragService;
        this.llmClient = llmClient;
        this.agentResultProducer = agentResultProducer;
        this.maxIterations = properties != null && properties.agent() != null && properties.agent().maxIterations() > 0
                ? properties.agent().maxIterations()
                : 3;
    }

    public AgentResultEvent handle(AgentTaskEvent task) {
        InvestigationOutcome outcome = runInvestigation(task, true, false);
        return outcome.result();
    }

    public AgentInvestigationReport inspect(AgentTaskEvent task) {
        InvestigationOutcome outcome = runInvestigation(task, false, true);
        return new AgentInvestigationReport(
                llmClient.getClass().getSimpleName(),
                resolveReasoningModel(),
                maxIterations,
                outcome.context().ragLogs(),
                outcome.context().historicalIncidents(),
                outcome.steps(),
                outcome.result()
        );
    }

    private InvestigationOutcome runInvestigation(AgentTaskEvent task, boolean publishResult, boolean captureTrace) {
        LOGGER.info(
                "Starting agent investigation incidentId={} service={} publishResult={} captureTrace={} maxIterations={} llmClient={}",
                task.incidentId(),
                task.service(),
                publishResult,
                captureTrace,
                maxIterations,
                llmClient.getClass().getSimpleName()
        );
        AgentContext context = ragService.initializeContext(task);
        LOGGER.info(
                "Initialized agent context incidentId={} ragLogs={} history={} priorToolResults={}",
                task.incidentId(),
                context.ragLogs().size(),
                context.historicalIncidents().size(),
                context.toolResults().size()
        );
        List<AgentInvestigationStep> steps = captureTrace ? new ArrayList<>() : List.of();

        for (int iteration = 1; iteration <= maxIterations; iteration++) {
            AgentDecision decision = llmClient.decide(context);
            LOGGER.info(
                    "Agent decision incidentId={} iteration={} actionType={}",
                    task.incidentId(),
                    iteration,
                    decision.actionType()
            );
            if (decision.actionType() == AgentActionType.FINAL) {
                AgentResultEvent result = finalResult(task, decision, iteration, false);
                LOGGER.info(
                        "Agent produced final decision incidentId={} iteration={} usedFallback={} recommendations={}",
                        task.incidentId(),
                        iteration,
                        false,
                        result.recommendations().size()
                );
                if (captureTrace) {
                    steps.add(AgentInvestigationStep.finalStep(
                            iteration,
                            decision.summary(),
                            decision.rootCause(),
                            decision.recommendations()
                    ));
                }
                if (publishResult) {
                    agentResultProducer.publish(result);
                }
                return new InvestigationOutcome(context, List.copyOf(steps), result);
            }

            try {
                LOGGER.info(
                        "Executing tool incidentId={} iteration={} tool={} input={}",
                        task.incidentId(),
                        iteration,
                        decision.toolCall().name(),
                        decision.toolCall().input()
                );
                ToolResult toolResult = toolExecutor.execute(decision.toolCall());
                context.addToolResult(toolResult);
                LOGGER.info(
                        "Tool completed incidentId={} iteration={} tool={} success={} summary={}",
                        task.incidentId(),
                        iteration,
                        toolResult.toolName(),
                        toolResult.success(),
                        toolResult.summary()
                );
                if (captureTrace) {
                    steps.add(AgentInvestigationStep.toolStep(
                            iteration,
                            decision.toolCall().name(),
                            decision.toolCall().input(),
                            toolResult.success(),
                            toolResult.summary()
                    ));
                }
            } catch (RuntimeException exception) {
                LOGGER.warn("Tool execution failed for incident {}", task.incidentId(), exception);
                ToolResult toolResult = ToolResult.builder()
                        .toolName(decision.toolCall().name())
                        .success(false)
                        .summary(exception.getMessage())
                        .payload(Map.of())
                        .build();
                context.addToolResult(toolResult);
                if (captureTrace) {
                    steps.add(AgentInvestigationStep.toolStep(
                            iteration,
                            decision.toolCall().name(),
                            decision.toolCall().input(),
                            toolResult.success(),
                            toolResult.summary()
                    ));
                }
            }
        }

        AgentResultEvent fallback = fallbackResult(task);
        LOGGER.info(
                "Agent exhausted iterations incidentId={} maxIterations={} usedFallback=true",
                task.incidentId(),
                maxIterations
        );
        if (captureTrace) {
            steps.add(AgentInvestigationStep.finalStep(
                    maxIterations,
                    fallback.summary(),
                    fallback.rootCause(),
                    fallback.recommendations()
            ));
        }
        if (publishResult) {
            agentResultProducer.publish(fallback);
        }
        return new InvestigationOutcome(context, List.copyOf(steps), fallback);
    }

    private AgentResultEvent finalResult(AgentTaskEvent task, AgentDecision decision, int iterations, boolean usedFallback) {
        return AgentResultEvent.builder()
                .incidentId(task.incidentId())
                .service(task.service())
                .summary(decision.summary())
                .rootCause(decision.rootCause())
                .recommendations(List.copyOf(decision.recommendations()))
                .iterations(iterations)
                .usedFallback(usedFallback)
                .completedAt(Instant.now())
                .build();
    }

    private AgentResultEvent fallbackResult(AgentTaskEvent task) {
        return AgentResultEvent.builder()
                .incidentId(task.incidentId())
                .service(task.service())
                .summary("The investigation completed without enough evidence for a high-confidence conclusion.")
                .rootCause("Unable to determine root cause from logs and incident history alone.")
                .recommendations(List.of(
                        "Review service metrics and recent deployments for additional context.",
                        "Inspect the latest error logs and dependency health before taking recovery action."
                ))
                .iterations(maxIterations)
                .usedFallback(true)
                .completedAt(Instant.now())
                .build();
    }

    private String resolveReasoningModel() {
        if (llmClient instanceof GeminiLlmClient geminiLlmClient) {
            return geminiLlmClient.modelName();
        }
        return "rule-based";
    }

    private record InvestigationOutcome(
            AgentContext context,
            List<AgentInvestigationStep> steps,
            AgentResultEvent result
    ) {
    }
}
