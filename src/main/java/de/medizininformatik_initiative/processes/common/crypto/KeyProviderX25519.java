package de.medizininformatik_initiative.processes.common.crypto;

import static org.hl7.fhir.r4.model.Bundle.BundleType.COLLECTION;
import static org.hl7.fhir.r4.model.DocumentReference.ReferredDocumentStatus.FINAL;
import static org.hl7.fhir.r4.model.Enumerations.DocumentReferenceStatus.CURRENT;

import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import de.medizininformatik_initiative.processes.common.util.ConstantsBase;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.client.dsf.DsfClient;

public class KeyProviderX25519 implements KeyProvider, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(KeyProviderX25519.class);

	private final PrivateKey privateKey;
	private final PublicKey publicKey;

	private final ProcessPluginApi api;

	public KeyProviderX25519(ProcessPluginApi api, PrivateKey privateKey, PublicKey publicKey)
	{
		this.api = api;
		this.privateKey = privateKey;
		this.publicKey = publicKey;
	}

	@Override
	public void afterPropertiesSet()
	{
		Objects.requireNonNull(api, "api");
	}

	@Override
	public void createPublicKeyIfNotExists(String receiverKeyId)
	{
		try
		{
			if (publicKey != null)
			{
				String baseUrl = api.getEndpointProvider().getLocalEndpointAddress();
				Optional<Bundle> bundleOnServer = readPublicKeyIfExists(receiverKeyId, baseUrl);

				byte[] hash = DigestUtils.sha256(publicKey.getEncoded());
				boolean createOrUpdate = true;

				if (bundleOnServer.isPresent())
				{
					if (hashMatches(hash, bundleOnServer.get()))
					{
						logger.info(
								"PublicKey Bundle for receiver-key-id '{}' already exists on DSF FHIR server with base Url '{}'",
								receiverKeyId, baseUrl);
						createOrUpdate = false;
					}
					else
						logger.info(
								"Updating PublicKey Bundle for receiver-key-id '{}' on DSF FHIR server with baseUrl '{}' because hash changed ...",
								receiverKeyId, baseUrl);
				}
				else
					logger.info(
							"Creating new PublicKey Bundle for receiver-key-id '{}' on DSF FHIR server with baseUrl '{}' ...",
							receiverKeyId, baseUrl);

				if (createOrUpdate)
					bundleOnServer = storePublicKeyBundle(receiverKeyId, hash);

				IdType bundleOnServerId = bundleOnServer.get().getIdElement();
				bundleOnServerId.setIdBase(baseUrl);
				logger.info("PublicKey Bundle for receiver-key-id '{}' has id '{}'", receiverKeyId,
						bundleOnServerId.getValue());
			}
		}
		catch (Exception exception)
		{
			throw new RuntimeException("Error while creating PublicKey Bundle for receiver-key-id ' " + receiverKeyId
					+ "' - " + exception.getMessage(), exception);
		}
	}

	@Override
	public Optional<Bundle> readPublicKeyIfExists(String receiverKeyId, String endpointUrl)
	{
		logger.info("Reading PublicKey Bundle for receiver-key-id '{}' on DSF FHIR server with baseUrl '{}' ...",
				receiverKeyId, endpointUrl);

		Bundle publicKeyBundle = api.getDsfClientProvider().getByEndpointUrl(endpointUrl).search(Bundle.class, Map.of(
				"identifier",
				Collections.singletonList(ConstantsBase.NAMINGSYSTEM_MII_RECEIVER_KEY_ID + "|" + receiverKeyId)));

		int total = publicKeyBundle.getTotal();

		if (total >= 1)
		{
			if (total > 1)
				logger.warn(
						"PublicKey Bundle for receiver-key-id '{}' on DSF FHIR server with baseUrl '{}' contains > 1 entries ({}), using the first",
						receiverKeyId, endpointUrl, total);

			return Optional.of((Bundle) publicKeyBundle.getEntryFirstRep().getResource());
		}
		else
		{
			logger.debug("PublicKey Bundle for receiver-key-id '{}' on DSF FHIR server with baseUrl '{}' is empty",
					receiverKeyId, endpointUrl);
			return Optional.empty();
		}
	}

	@Override
	public void deletePublicKeyIfExists(String receiverKeyId)
	{
		DsfClient client = api.getDsfClientProvider().getLocal();

		String baseUrl = client.getBaseUrl();
		Optional<Bundle> bundleOnServer = readPublicKeyIfExists(receiverKeyId, baseUrl);

		if (bundleOnServer.isPresent())
		{
			logger.info("Deleting PublicKey Bundle for receiver-key-id '{}' on DSF FHIR server with baseUrl '{}' ...",
					receiverKeyId, baseUrl);

			IdType idType = bundleOnServer.get().getIdElement();
			client.delete(Bundle.class, idType.getIdPart());
			client.deletePermanently(Bundle.class, idType.getIdPart());
		}
		else
		{
			logger.info(
					"Could not delete PublicKey Bundle for receiver-key-id '{}' on DSF FHIR server with baseUrl '{}' - does not exist",
					receiverKeyId, baseUrl);
		}
	}

	private boolean hashMatches(byte[] hash, Bundle bundleOnServer)
	{
		return bundleOnServer.getEntry().stream().filter(Bundle.BundleEntryComponent::hasResource)
				.map(Bundle.BundleEntryComponent::getResource).filter(r -> r instanceof DocumentReference)
				.map(r -> (DocumentReference) r).filter(DocumentReference::hasContent)
				.flatMap(dr -> dr.getContent().stream())
				.filter(DocumentReference.DocumentReferenceContentComponent::hasAttachment)
				.map(DocumentReference.DocumentReferenceContentComponent::getAttachment).filter(Attachment::hasHash)
				.map(Attachment::getHash).anyMatch(h -> MessageDigest.isEqual(hash, h));
	}

	private Optional<Bundle> storePublicKeyBundle(String receiverKeyId, byte[] hash)
	{
		Bundle bundleToCreate = createPublicKeyBundle(receiverKeyId, hash);
		return Optional.of(api.getDsfClientProvider().getLocal().updateConditionaly(bundleToCreate,
				Map.of("identifier", List.of(ConstantsBase.NAMINGSYSTEM_MII_RECEIVER_KEY_ID + "|" + receiverKeyId))));
	}

	private Bundle createPublicKeyBundle(String receiverKeyId, byte[] hash)
	{
		Date date = new Date();

		Binary binary = new Binary().setContentType("application/pem-certificate-chain");
		binary.setContent(getPublicKey().getEncoded());

		DocumentReference documentReference = new DocumentReference().setStatus(CURRENT).setDocStatus(FINAL);
		documentReference.getMasterIdentifier().setSystem(ConstantsBase.NAMINGSYSTEM_MII_RECEIVER_KEY_ID)
				.setValue(receiverKeyId);
		documentReference.addAuthor().setType(ResourceType.Organization.name())
				.setIdentifier(api.getOrganizationProvider().getLocalOrganizationIdentifier().get());
		documentReference.setDate(date);

		String binaryUuid = "urn:uuid:" + UUID.randomUUID().toString();
		documentReference.addContent().getAttachment().setContentType("application/pem-certificate-chain")
				.setUrl(binaryUuid).setHash(hash);

		Bundle bundle = new Bundle().setType(COLLECTION);
		bundle.getIdentifier().setSystem(ConstantsBase.NAMINGSYSTEM_MII_RECEIVER_KEY_ID).setValue(receiverKeyId);
		bundle.setTimestamp(date);
		bundle.addEntry().setResource(documentReference).setFullUrl("urn:uuid:" + UUID.randomUUID().toString());
		bundle.addEntry().setResource(binary).setFullUrl(binaryUuid);

		api.getReadAccessHelper().addAll(bundle);

		api.getDataLogger().log("Created PublicKey Bundle", bundle);

		return bundle;
	}

	@Override
	public PrivateKey getPrivateKey()
	{
		return privateKey;
	}

	@Override
	public PublicKey getPublicKey()
	{
		return publicKey;
	}
}
