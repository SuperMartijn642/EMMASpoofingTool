package emma.ssl;

import httpserver.HTTPServer;

import java.io.*;
import java.util.*;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

public class HttpResponse {

    private String protocol;
    private int statusCode;
    private String status;
    private HashMap<String, List<String>> headers;
    private int contentLength;
    private String body;

    private OutputStream[] encoders = new OutputStream[4];

    private InputStream streamBody;
    public HTTPServer.Headers customHeaders;

    public HttpResponse() {
        this.headers = new HashMap<>();
    }

    public HttpResponse(InputStream in) throws IOException {
        this.headers = new HashMap<>();
        String line = HTTPServer.readLine(in);
        String[] firstLines = line.split(" ", 3);
        this.setProtocol(firstLines[0]);
        this.setStatusCode(Integer.parseInt(firstLines[1]));
        this.setStatus(firstLines[2]);

        this.customHeaders = HTTPServer.readHeaders(in);

        String header = customHeaders.get("Transfer-Encoding");
        if (header != null && !header.toLowerCase(Locale.US).equals("identity")) {
            if (Arrays.asList(HTTPServer.splitElements(header, true)).contains("chunked"))
                streamBody = new HTTPServer.ChunkedInputStream(in, customHeaders);
            else
                streamBody = in; // body ends when connection closes
        } else {
            header = customHeaders.get("Content-Length");
            long len = header == null ? 0 : HTTPServer.parseULong(header, 10);
            streamBody = new HTTPServer.LimitedInputStream(in, len, false);
        }
    }

    public HttpResponse(String firstLine, BufferedReader inputStream) throws IOException {
        this.headers = new HashMap<>();

        String line = firstLine;
        String[] firstLines = line.split(" ", 3);
        this.setProtocol(firstLines[0]);
        this.setStatusCode(Integer.parseInt(firstLines[1]));
        this.setStatus(firstLines[2]);

        while (!(line = inputStream.readLine()).equals("")) {
            String[] split = line.split(": ", 2);
            String[] values = split[1].split("; ");
            this.setHeader(split[0], new ArrayList<>(Arrays.asList(values)));
        }

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
    }

    public String toString() {
        StringBuilder output = new StringBuilder();
        output.append(this.getProtocol() + " " + this.getStatusCode() + " " + this.getStatus() + "\r\n");

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

    public void writeTo(OutputStream out) throws IOException {
        String firstLine = this.getProtocol() + " " + this.getStatusCode() + " " + this.getStatus() + "\r\n";
        out.write(firstLine.getBytes());
        customHeaders.writeTo(out);
        if (streamBody != null) {
            streamBody.transferTo(out);
        }
        out.write("\r\n\r\n".getBytes());
        out.close();
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
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

    public int getStatusCode() {
        return this.statusCode;
    }

    public String getStatus() {
        return this.status;
    }

    public String getProtocol() {
        return this.protocol;
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
        return this.headers.keySet().contains(header);
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

    public OutputStream getStreamBody(OutputStream out) throws IOException {
        if (encoders[0] != null)
            return encoders[0]; // return the existing stream (or null)
        // set up chain of encoding streams according to headers
        List<String> te = Arrays.asList(HTTPServer.splitElements(customHeaders.get("Transfer-Encoding"), true));
        List<String> ce = Arrays.asList(HTTPServer.splitElements(customHeaders.get("Content-Encoding"), true));
        int i = encoders.length - 1;
        encoders[i] = new FilterOutputStream(out) {
            @Override
            public void close() {} // keep underlying connection stream open for now
            @Override // override the very inefficient default implementation
            public void write(byte[] b, int off, int len) throws IOException { out.write(b, off, len); }
        };
        if (te.contains("chunked"))
            encoders[--i] = new HTTPServer.ChunkedOutputStream(encoders[i + 1]);
        if (ce.contains("gzip") || te.contains("gzip"))
            encoders[--i] = new GZIPOutputStream(encoders[i + 1], 4096);
        else if (ce.contains("deflate") || te.contains("deflate"))
            encoders[--i] = new DeflaterOutputStream(encoders[i + 1]);
        encoders[0] = encoders[i];
        encoders[i] = null; // prevent duplicate reference
        return encoders[0]; // returned stream is always first
    }

    public void sendBody(InputStream body, long length, long[] range, OutputStream output) throws IOException {
        OutputStream out = getStreamBody(output);
        if (out != null) {
            if (range != null) {
                long offset = range[0];
                length = range[1] - range[0] + 1;
                while (offset > 0) {
                    long skip = body.skip(offset);
                    if (skip == 0)
                        throw new IOException("can't skip to " + range[0]);
                    offset -= skip;
                }
            }
            HTTPServer.transfer(body, out, length);
        }
    }
}
