package STARTER.Configuration;

import STARTER.Models.AdminMailboxMessage;
import STARTER.Repositories.AdminMailboxMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SupportMailboxBackfillConfig implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(SupportMailboxBackfillConfig.class);
    private final AdminMailboxMessageRepository mailboxMessageRepository;

    @Value("${app.admin.username:admin}")
    private String primaryAdminUsername;

    public SupportMailboxBackfillConfig(AdminMailboxMessageRepository mailboxMessageRepository) {
        this.mailboxMessageRepository = mailboxMessageRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int updated = 0;

        Map<String, List<AdminMailboxMessage>> byRecipient = mailboxMessageRepository.findAll()
                .stream()
                .collect(Collectors.groupingBy(AdminMailboxMessage::getRecipientUsername));

        for (List<AdminMailboxMessage> thread : byRecipient.values()) {

            thread.sort(Comparator.comparing(AdminMailboxMessage::getCreatedAt));
            String lastAdminSender = primaryAdminUsername.trim();

            for (AdminMailboxMessage message : thread) {

                if (message.isFromAdmin()) {
                    lastAdminSender = message.getSenderUsername();
                }

                boolean userReply = message.getSenderUsername().equals(message.getRecipientUsername());

                if (userReply && message.isFromAdmin()) {

                    message.setFromAdmin(false);
                    message.setReadByRecipient(true);
                    message.setReadByAdmin(false);
                    updated++;

                } else if (!userReply && !message.isFromAdmin()) {

                    message.setFromAdmin(true);
                    message.setReadByAdmin(true);
                    updated++;
                }

                if (!message.isFromAdmin()
                        && (message.getAdminRecipientUsername() == null
                        || message.getAdminRecipientUsername().isBlank())) {

                    message.setAdminRecipientUsername(lastAdminSender);
                    updated++;
                }
            }
        }

        if (updated > 0) {

            mailboxMessageRepository.saveAll(mailboxMessageRepository.findAll());
            logger.info("Backfilled {} admin mailbox message field(s).", updated);
        }
    }
}
