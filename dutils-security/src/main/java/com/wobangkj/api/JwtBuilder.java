package com.wobangkj.api;

import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.SignatureGenerationException;
import com.auth0.jwt.impl.ClaimsHolder;
import com.auth0.jwt.impl.PayloadSerializer;
import com.auth0.jwt.impl.PublicClaims;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 构建jwt
 *
 * @author cliod
 * @since 9/28/20 4:47 PM
 */
public class JwtBuilder {

	private final Algorithm algorithm;
	private final String headerJson;
	private final String payloadJson;

	private JwtBuilder(Algorithm algorithm, Map<String, Object> headerClaims, Map<String, Object> payloadClaims) throws JWTCreationException {
		this.algorithm = algorithm;
		try {
			ObjectMapper mapper = new ObjectMapper();
			SimpleModule module = new SimpleModule();
			module.addSerializer(ClaimsHolder.class, new PayloadSerializer());
			mapper.registerModule(module);
			mapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
			headerJson = mapper.writeValueAsString(headerClaims);
			payloadJson = mapper.writeValueAsString(new ClaimsHolder(payloadClaims));
		} catch (JsonProcessingException e) {
			throw new JWTCreationException("Some of the Claims couldn't be converted to a valid JSON format.", e);
		}
	}


	/**
	 * Initialize a JWTCreator instance.
	 *
	 * @return a JWTCreator.Builder instance to configure.
	 */
	static Builder init() {
		return new Builder();
	}

	private String sign() throws SignatureGenerationException {
		String header = Base64.getEncoder().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
		String payload = Base64.getEncoder().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

		byte[] signatureBytes = algorithm.sign(header.getBytes(StandardCharsets.UTF_8), payload.getBytes(StandardCharsets.UTF_8));
		String signature = Base64.getEncoder().encodeToString(signatureBytes);

		return String.format("%s.%s.%s", header, payload, signature);
	}

	/**
	 * The Builder class holds the Claims that defines the JWT to be created.
	 */
	public static class Builder implements Buildable {
		private final Map<String, Object> payloadClaims;
		private Map<String, Object> headerClaims;

		Builder() {
			this.payloadClaims = new HashMap<>();
			this.headerClaims = new HashMap<>();
		}

		/**
		 * Add specific Claims to set as the Header.
		 *
		 * @param headerClaims the values to use as Claims in the token's Header.
		 * @return this same Builder instance.
		 */
		@Override
		public Builder withHeader(Map<String, Object> headerClaims) {
			this.headerClaims = new HashMap<>(headerClaims);
			return this;
		}

		/**
		 * Add a specific Key Id ("kid") claim to the Header.
		 * If the {@link Algorithm} used to sign this token was instantiated with a KeyProvider, the 'kid' value will be taken from that provider and this one will be ignored.
		 *
		 * @param keyId the Key Id value.
		 * @return this same Builder instance.
		 */
		@Override
		public Builder withKeyId(String keyId) {
			this.headerClaims.put(PublicClaims.KEY_ID, keyId);
			return this;
		}

		/**
		 * Add a specific Issuer ("iss") claim to the Payload.
		 *
		 * @param issuer the Issuer value.
		 * @return this same Builder instance.
		 */
		@Override
		public Builder withIssuer(String issuer) {
			addClaim(PublicClaims.ISSUER, issuer);
			return this;
		}

		/**
		 * Add a specific Subject ("sub") claim to the Payload.
		 *
		 * @param subject the Subject value.
		 * @return this same Builder instance.
		 */
		@Override
		public Builder withSubject(String subject) {
			addClaim(PublicClaims.SUBJECT, subject);
			return this;
		}

		/**
		 * Add a specific Audience ("aud") claim to the Payload.
		 *
		 * @param audience the Audience value.
		 * @return this same Builder instance.
		 */
		@Override
		public Builder withAudience(String... audience) {
			addClaim(PublicClaims.AUDIENCE, audience);
			return this;
		}

		/**
		 * Add a specific Expires At ("exp") claim to the Payload.
		 *
		 * @param expiresAt the Expires At value.
		 * @return this same Builder instance.
		 */
		@Override
		public Builder withExpiresAt(Date expiresAt) {
			addClaim(PublicClaims.EXPIRES_AT, expiresAt);
			return this;
		}

		/**
		 * Add a specific Not Before ("nbf") claim to the Payload.
		 *
		 * @param notBefore the Not Before value.
		 * @return this same Builder instance.
		 */
		@Override
		public Builder withNotBefore(Date notBefore) {
			addClaim(PublicClaims.NOT_BEFORE, notBefore);
			return this;
		}

		/**
		 * Add a specific Issued At ("iat") claim to the Payload.
		 *
		 * @param issuedAt the Issued At value.
		 * @return this same Builder instance.
		 */
		@Override
		public Builder withIssuedAt(Date issuedAt) {
			addClaim(PublicClaims.ISSUED_AT, issuedAt);
			return this;
		}

		/**
		 * Add a specific JWT Id ("jti") claim to the Payload.
		 *
		 * @param jwtId the Token Id value.
		 * @return this same Builder instance.
		 */
		@Override
		public Builder withJWTId(String jwtId) {
			addClaim(PublicClaims.JWT_ID, jwtId);
			return this;
		}

		/**
		 * Add a custom Claim value.
		 *
		 * @param name  the Claim's name.
		 * @param value the Claim's value.
		 * @return this same Builder instance.
		 * @throws IllegalArgumentException if the name is null.
		 */
		@Override
		public Builder withClaim(String name, Object value) throws IllegalArgumentException {
			assertNonNull(name);
			addClaim(name, value);
			return this;
		}

		/**
		 * Creates a new JWT and signs is with the given algorithm
		 *
		 * @param algorithm used to sign the JWT
		 * @return a new JWT token
		 * @throws IllegalArgumentException if the provided algorithm is null.
		 * @throws JWTCreationException     if the claims could not be converted to a valid JSON or there was a problem with the signing key.
		 */
		@Override
		public String sign(Algorithm algorithm) throws IllegalArgumentException, JWTCreationException {
			if (algorithm == null) {
				throw new IllegalArgumentException("The Algorithm cannot be null.");
			}
			headerClaims.put(PublicClaims.ALGORITHM, algorithm.getName());
			headerClaims.put(PublicClaims.TYPE, "JWT");
			String signingKeyId = algorithm.getSigningKeyId();
			if (signingKeyId != null) {
				withKeyId(signingKeyId);
			}
			return new JwtBuilder(algorithm, headerClaims, payloadClaims).sign();
		}

		private void assertNonNull(String name) {
			if (name == null) {
				throw new IllegalArgumentException("The Custom Claim's name can't be null.");
			}
		}

		private void addClaim(String name, Object value) {
			if (value == null) {
				payloadClaims.remove(name);
				return;
			}
			payloadClaims.put(name, value);
		}
	}
}
