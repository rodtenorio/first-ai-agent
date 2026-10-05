package tenorio.rodrigo.ai_agent;

import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import org.springaicommunity.agent.tools.FileSystemTools;
import org.springaicommunity.agent.tools.GlobTool;
import org.springaicommunity.agent.tools.GrepTool;
import org.springaicommunity.agent.tools.ShellTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

@SpringBootApplication
public class AiAgentApplication {

	public static void main(String[] args) {
		var application = new SpringApplication(AiAgentApplication.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.run(args);
	}

	@Bean 
	CommandLineRunner run(ChatClient.Builder chatClientBuilder) {
		return args -> {
			ChatClient chatClient = chatClientBuilder.defaultSystem("""
					You are a helpful coding assistant. You hava access to tools for reading files,
					searching code, running shell commands, and editing files.
					Use them to help the user with their codebase.

					Current directory: %s
					""".formatted(System.getProperty("user.dir")))
					.defaultTools(
						FileSystemTools.builder().build(),
						GrepTool.builder().build(),
						ShellTools.builder().build(),
						GlobTool.builder().build())
					.defaultAdvisors(
						MessageChatMemoryAdvisor.builder(
							MessageWindowChatMemory.builder().maxMessages(500).build()
						).order(Ordered.HIGHEST_PRECEDENCE + 1000).build())
					.build();

			var conversationId = UUID.randomUUID().toString();

			var scanner = new Scanner(System.in);
			System.out.println("🦾 Coding agent. Ask me anything: ");
			while (scanner.hasNextLine()) {
				String userInput = scanner.nextLine();
				if (userInput.equals("exit")) {
					break;
				}
				var chatResponse = chatClient.prompt(userInput)
					.advisors(advisorSpec -> advisorSpec.param(
						ChatMemory.CONVERSATION_ID, conversationId))
					.toolContext(Map.of("workingDirectory", System.getProperty("user.dir")))
					.call().chatResponse();
				System.out.println("Model: " + chatResponse.getMetadata().getModel());
				System.out.println("Prompt tokens: " + chatResponse.getMetadata().getUsage().getPromptTokens());
				System.out.println("Completion tokens: " + chatResponse.getMetadata().getUsage().getCompletionTokens());
				System.out.println("Total tokens: " + chatResponse.getMetadata().getUsage().getTotalTokens());
				
				System.out.println(chatResponse.getResult().getOutput().getText());
			}
			scanner.close();
		};
	}
}
