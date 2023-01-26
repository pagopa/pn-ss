package it.pagopa.pnss.repositoryManager.service.impl;

import java.util.Iterator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.pagopa.pnss.repositoryManager.constant.DynamoTableNameConstant;
import it.pagopa.pnss.repositoryManager.dto.UserConfigurationInput;
import it.pagopa.pnss.repositoryManager.dto.UserConfigurationOutput;
import it.pagopa.pnss.repositoryManager.entity.UserConfigurationEntity;
import it.pagopa.pnss.repositoryManager.exception.RepositoryManagerException;
import it.pagopa.pnss.repositoryManager.service.UserConfigurationService;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

@Service
public class UserConfigurationServiceImpl implements UserConfigurationService {

	@Autowired
	private DynamoDbEnhancedClient enhancedClient;
	@Autowired
	private ObjectMapper objectMapper;


	public UserConfigurationOutput getUser(String name) {
		try {
			DynamoDbTable<UserConfigurationEntity> userConfigurationTable = enhancedClient.table(DynamoTableNameConstant.ANAGRAFICA_CLIENT_TABLE_NAME,
					TableSchema.fromBean(UserConfigurationEntity.class));
			QueryConditional queryConditional = QueryConditional.keyEqualTo(Key.builder().partitionValue(name).build());

			Iterator<UserConfigurationEntity> result = userConfigurationTable.query(queryConditional).items()
					.iterator();

			UserConfigurationEntity user = result.next();

			return objectMapper.convertValue(user, UserConfigurationOutput.class);

		} catch (DynamoDbException e) {
			System.err.println(e.getMessage());
			throw new RepositoryManagerException.DynamoDbException();
		}
	}

	public UserConfigurationOutput postUser(UserConfigurationInput userInput) {

		try {
			DynamoDbTable<UserConfigurationEntity> userConfigurationTable = enhancedClient.table(DynamoTableNameConstant.ANAGRAFICA_CLIENT_TABLE_NAME,
					TableSchema.fromBean(UserConfigurationEntity.class));

			UserConfigurationEntity userEntity = objectMapper.convertValue(userInput, UserConfigurationEntity.class);

			if (userConfigurationTable.getItem(userEntity) == null) {

				userConfigurationTable.putItem(userEntity);
				System.out.println("User inserito nel db ");

				return objectMapper.convertValue(userEntity, UserConfigurationOutput.class);

			} else {
				System.out.println("L'utente non può essere aggiunto, id già esistente");
				throw new RepositoryManagerException.IdClientAlreadyPresent(userInput.getName());

			}
		} catch (DynamoDbException e) {
			System.err.println(e.getMessage());
			throw new RepositoryManagerException.DynamoDbException();

		}

	}

	public UserConfigurationOutput updateUser(UserConfigurationInput user) {

		try {
			DynamoDbTable<UserConfigurationEntity> userConfigurationTable = enhancedClient.table(DynamoTableNameConstant.ANAGRAFICA_CLIENT_TABLE_NAME,
					TableSchema.fromBean(UserConfigurationEntity.class));
			UserConfigurationEntity userEntity = objectMapper.convertValue(user, UserConfigurationEntity.class);

			if (userConfigurationTable.getItem(userEntity) != null) {
				userConfigurationTable.putItem(userEntity);
				System.out.println("Modifica avvenuta con successo");
				return objectMapper.convertValue(userEntity, UserConfigurationOutput.class);
			} else {
				throw new RepositoryManagerException.DynamoDbException();
			}
		} catch (DynamoDbException e) {
			System.err.println(e.getMessage());
			throw new RepositoryManagerException.DynamoDbException();
		}
	}

	public UserConfigurationOutput deleteUser(String name) {

		try {
			DynamoDbTable<UserConfigurationEntity> userConfigurationTable = enhancedClient.table(DynamoTableNameConstant.ANAGRAFICA_CLIENT_TABLE_NAME,
					TableSchema.fromBean(UserConfigurationEntity.class));
			QueryConditional queryConditional = QueryConditional.keyEqualTo(Key.builder().partitionValue(name).build());
			Iterator<UserConfigurationEntity> result = userConfigurationTable.query(queryConditional).items()
					.iterator();
			UserConfigurationEntity userEntity = result.next();
			userConfigurationTable.deleteItem(userEntity);
			System.out.println("Cancellazione avvenuta con successo");
			return objectMapper.convertValue(userEntity, UserConfigurationOutput.class);

		} catch (DynamoDbException e) {
			System.err.println(e.getMessage());
			throw new RepositoryManagerException.DynamoDbException();

		}
	}

}
