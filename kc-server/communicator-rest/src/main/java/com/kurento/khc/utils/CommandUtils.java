package com.kurento.khc.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kurento.khc.datamodel.CommandTransactionDao;

@Component("khcCommandCleaner")
public class CommandUtils {

	@Value("${kurento.command.transaction-ttl-milliseconds:#{null}}")
	private Long TRANSACTION_TTL_MILLISECONDS = 3600L * 12L;

	@Autowired
	private CommandTransactionDao transactionDao;


	@Scheduled(fixedDelay = 60000)
	public void cleanCommandQeueue() {
		// Clean until 0 hits is reached
		while (transactionDao.cleanOldTransactions(TRANSACTION_TTL_MILLISECONDS, 50) > 0) {
		}
	}
}
