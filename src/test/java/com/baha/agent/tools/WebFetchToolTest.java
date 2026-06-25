package com.baha.agent.tools;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.InetAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WebFetchToolTest {

    // Real resolver: IP literals + localhost resolve with no network.
    private final WebFetchTool guardTool = new WebFetchTool(RestClient.builder());

    @Test
    void blocksNonHttpScheme() {
        assertThat(guardTool.fetchUrl("ftp://example.com/file")).startsWith("Blocked");
        assertThat(guardTool.fetchUrl("file:///etc/passwd")).startsWith("Blocked");
    }

    @Test
    void blocksLocalhostByName() {
        assertThat(guardTool.fetchUrl("http://localhost:8080/secret")).startsWith("Blocked");
    }

    @Test
    void blocksLoopbackIpv4() {
        assertThat(guardTool.fetchUrl("http://127.0.0.1/")).startsWith("Blocked");
    }

    @Test
    void blocksLoopbackIpv6() {
        assertThat(guardTool.fetchUrl("http://[::1]/")).startsWith("Blocked");
    }

    @Test
    void blocksCloudMetadataLinkLocal() {
        assertThat(guardTool.fetchUrl("http://169.254.169.254/latest/meta-data/")).startsWith("Blocked");
    }

    @Test
    void blocksPrivateRanges() {
        assertThat(guardTool.fetchUrl("http://10.0.0.5/")).startsWith("Blocked");
        assertThat(guardTool.fetchUrl("http://192.168.1.1/")).startsWith("Blocked");
        assertThat(guardTool.fetchUrl("http://172.16.0.1/")).startsWith("Blocked");
    }

    @Test
    void rejectsMalformedUrl() {
        assertThat(guardTool.fetchUrl("not a url")).startsWith("Blocked");
    }

    @Test
    void fetchesPublicUrlAndReturnsText() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        // Fake resolver maps the public host to a public IP literal — no DNS.
        WebFetchTool tool = new WebFetchTool(builder.build(),
                host -> new InetAddress[]{InetAddress.getByName("93.184.216.34")});

        server.expect(requestTo(containsString("example.com")))
                .andRespond(withSuccess("<html><body><p>Hello World</p></body></html>",
                        MediaType.TEXT_HTML));

        String result = tool.fetchUrl("http://example.com/page");

        assertThat(result).contains("Hello World");
        server.verify();
    }
}
