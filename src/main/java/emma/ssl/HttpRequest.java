package emma.ssl;

import httpserver.HTTPServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class HttpRequest {

    private String method;
    private String protocol;
    private String path;
    private HashMap<String, List<String>> headers;
    private int contentLength;
    private String body;
    private InputStream inputBody;

    public HTTPServer.Headers customHeaders;

    public HttpRequest() {
        this.headers = new HashMap<>();
    }

    public HttpRequest(InputStream in) throws IOException {
        this.headers = new HashMap<>();
        String line = HTTPServer.readLine(in);
        String[] firstLines = line.split(" ", 3);
        this.setMethod(firstLines[0]);
        this.setPath(firstLines[1]);
        this.setProtocol(firstLines[2]);

        this.customHeaders = HTTPServer.readHeaders(in);

        String header = customHeaders.get("Transfer-Encoding");
        if (header != null && !header.toLowerCase(Locale.US).equals("identity")) {
            if (Arrays.asList(HTTPServer.splitElements(header, true)).contains("chunked"))
                inputBody = new HTTPServer.ChunkedInputStream(in, customHeaders);
            else
                inputBody = in; // body ends when connection closes
        } else {
            header = customHeaders.get("Content-Length");
            long len = header == null ? 0 : HTTPServer.parseULong(header, 10);
            inputBody = new HTTPServer.LimitedInputStream(in, len, false);
        }
    }

    public void writeTo(OutputStream out) throws IOException {
        String firstLine = this.getMethod() + " " + this.getPath() + " " + this.getProtocol() + "\r\n";
        out.write(firstLine.getBytes());
        customHeaders.writeTo(out);
        if (inputBody != null) {
            inputBody.transferTo(out);
        }
        out.write("\r\n\r\n".getBytes());
    }

    public HttpRequest(String firstLine, BufferedReader inputStream) throws IOException {
        this.headers = new HashMap<>();

        String line = firstLine;
        String[] firstLines = line.split(" ", 3);
        this.setMethod(firstLines[0]);
        this.setPath(firstLines[1]);
        this.setProtocol(firstLines[2]);

        while (!(line = inputStream.readLine()).equals("")) {
            String[] split = line.split(": ", 2);
            String[] values = split[1].split("; ");
            this.setHeader(split[0], new ArrayList<>(Arrays.asList(values)));
        }

        if (this.hasHeader("Content-Type")) {
            String contentType = this.getHeader("Content-Type").get(0);
            if (contentType.startsWith("text/")) {
                if (this.hasHeader("Content-Length")) {
                    this.setContentLength(Integer.parseInt(this.getHeader("Content-Length").get(0)));
                    int count = 0;
                    StringBuilder bodyBuilder = new StringBuilder();
                    while (count < this.getContentLength()) {
                        char c = (char) inputStream.read();
                        count++;
                        bodyBuilder.append(c);
                    }

                    this.setBody(bodyBuilder.toString());
                } else {
                    this.setContentLength(0);
                }
            } else if (contentType.startsWith("application/")) {

            } else {
                this.setContentLength(0);
            }
        }
    }

    public String toString() {
        StringBuilder output = new StringBuilder();
        output.append(this.getMethod() + " " + this.getPath() + " " + this.getProtocol() + "\r\n");

        for (Map.Entry<String, List<String>> entry : this.getHeaders().entrySet()) {
            output.append(this.headerToString(entry) + "\r\n");
        }

        output.append("\r\n");
        if (this.getBody() != null) {
            output.append(this.getBody());
        }
        output.append("\r\n");
        return output.toString();
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setHeader(String header, List<String> value) {
        this.headers.put(header, value);
    }

    public void setHeader(String header, String value) {
        if (this.hasHeader(header)) {
            this.getHeaders().get(header).clear();
            this.getHeaders().get(header).add(value);
        } else {
            this.getHeaders().put(header, new ArrayList<>(Arrays.asList(new String[]{value})));
        }
    }

    public void addHeader(String header, String value) {
        if (this.hasHeader(header)) {
            this.getHeaders().get(header).add(value);
        } else {
            this.getHeaders().put(header, new ArrayList<>(Arrays.asList(new String[]{value})));
        }
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    public String getMethod() {
        return this.method;
    }

    public String getProtocol() {
        return this.protocol;
    }

    public String getPath() {
        return this.path;
    }

    public HashMap<String, List<String>> getHeaders() {
        return this.headers;
    }

    public List<String> getHeader(String header) {
        return this.headers.get(header);
    }

    public String getBody() {
        return this.body;
    }

    public int getContentLength() {
        return this.contentLength;
    }

    public boolean hasHeader(String header) {
        return this.getHeaders().keySet().contains(header);
    }

    public boolean hasHeader(String header, String value) {
        if (hasHeader(header)) {
            return this.getHeader(header).contains(value);
        }
        return false;
    }

    public void removeHeader(String header) {
        this.getHeaders().remove(header);
    }

    public String headerToString(Map.Entry<String, List<String>> header) {
        StringBuilder builder = new StringBuilder(header.getKey() + ": " + header.getValue().get(0));
        for (int i = 1; i < header.getValue().size(); i++) {
            builder.append("; " + header.getValue().get(i));
        }
        return builder.toString();
    }

    public String headersToString() {
        StringBuilder builder = new StringBuilder();
        for (HTTPServer.Header header : customHeaders) {
            builder.append(header.getName() + ": " + header.getValue() + "\r\n");
        }
        return builder.toString();
    }
}
