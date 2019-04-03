package ru.gravit.launcher.downloader;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

public class ListDownloader {
    public void download(String base, List<String> applies, Path dstDirFile) throws IOException, URISyntaxException {
        try (CloseableHttpClient httpclient = HttpClients.custom()
                .setRedirectStrategy(new LaxRedirectStrategy())
                .build()) {

            HttpGet get = null;
            for (String apply : applies) {
                URI u = new URL(base.concat(apply)).toURI();
                LogHelper.debug("Download URL: %s", u.toString());
                if (get == null) get = new HttpGet(u);
                else {
                    get.reset();
                    get.setURI(u);
                }
                httpclient.execute(get, new FileDownloadResponseHandler(dstDirFile.resolve(apply)));
            }
        }
    }

    static class FileDownloadResponseHandler implements ResponseHandler<Path> {
        private final Path target;

        public FileDownloadResponseHandler(Path target) {
            this.target = target;
        }

        @Override
        public Path handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
            InputStream source = response.getEntity().getContent();
            IOHelper.transfer(source, this.target);
            return this.target;
        }
    }
}