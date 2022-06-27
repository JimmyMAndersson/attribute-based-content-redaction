package se.applyn.restapi.server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;

@Component
@Order(1)
public class TimeInstrumentationFilter extends OncePerRequestFilter {
    @Value(value = "${rest-api.log-path}")
    private String logPath;

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain) throws ServletException, IOException {
        final Long start = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            final Long executionTime = System.nanoTime() - start;

            DecimalFormat formatter = new DecimalFormat("#");
            formatter.setMaximumFractionDigits(8);
            formatter.setMinimumIntegerDigits(1);

            Path logURL = Paths.get(this.logPath).toAbsolutePath().normalize();
            FileWriter logFile = new FileWriter(logURL.toString(), true);

            StringBuilder builder = new StringBuilder();
            builder.append(request.getRequestURI());
            builder.append(",");
            builder.append(formatter.format(executionTime.doubleValue() * 1e-9));
            builder.append("\n");

            logFile.append(builder.toString());
            logFile.close();
        }
    }
}