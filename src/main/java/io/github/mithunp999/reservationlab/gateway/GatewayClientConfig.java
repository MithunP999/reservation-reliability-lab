package io.github.mithunp999.reservationlab.gateway;

import java.net.http.HttpClient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class GatewayClientConfig {

	/**
	 * Built explicitly, not from Boot's default client: HTTP/1.1 only (the default JDK client tries an HTTP/2 upgrade,
	 * which WireMock's Jetty answers with RST_STREAM), with explicit connect and read timeouts.
	 */
	@Bean
	RestClient paymentGatewayRestClient(GatewayProperties properties) {
		HttpClient httpClient = HttpClient.newBuilder()
				.version(HttpClient.Version.HTTP_1_1)
				.connectTimeout(properties.connectTimeout())
				.build();
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
		factory.setReadTimeout(properties.readTimeout());
		return RestClient.builder().requestFactory(factory).baseUrl(properties.baseUrl()).build();
	}

	/** The only real wait in the app: backoff between gateway retries. */
	@Bean
	Sleeper sleeper() {
		return Thread::sleep;
	}
}
