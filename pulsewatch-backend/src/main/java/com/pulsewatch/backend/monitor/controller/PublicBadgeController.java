package com.pulsewatch.backend.monitor.controller;

import com.pulsewatch.backend.monitor.entity.Monitor;
import com.pulsewatch.backend.monitor.repository.MonitorRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/public/badge")
@Tag(name = "Public Badges", description = "Public health badge SVG endpoints")
public class PublicBadgeController {

    @Autowired
    private MonitorRepository monitorRepository;

    @GetMapping(value = "/{id}", produces = "image/svg+xml")
    @Operation(summary = "Get a public SVG health badge for a monitor")
    public ResponseEntity<String> getBadge(@PathVariable UUID id) {
        Optional<Monitor> monitorOpt = monitorRepository.findById(id);

        String statusText;
        String color;
        int valueWidth;

        if (monitorOpt.isEmpty()) {
            statusText = "NOT FOUND";
            color = "#ef4444"; // red
            valueWidth = 70;
        } else {
            Monitor m = monitorOpt.get();
            if (!m.isActive()) {
                statusText = "PAUSED";
                color = "#6b7280"; // gray
                valueWidth = 55;
            } else {
                String status = m.getCurrentStatus();
                if ("UP".equalsIgnoreCase(status)) {
                    statusText = "UP";
                    color = "#10b981"; // green
                    valueWidth = 30;
                } else if ("DOWN".equalsIgnoreCase(status)) {
                    statusText = "DOWN";
                    color = "#ef4444"; // red
                    valueWidth = 45;
                } else if ("MAINTENANCE".equalsIgnoreCase(status)) {
                    statusText = "MAINTENANCE";
                    color = "#f59e0b"; // amber
                    valueWidth = 90;
                } else {
                    statusText = "UNKNOWN";
                    color = "#9ca3af"; // light gray
                    valueWidth = 65;
                }
            }
        }

        int labelWidth = 75;
        int totalWidth = labelWidth + valueWidth;
        double labelTextX = labelWidth / 2.0;
        double valueTextX = labelWidth + (valueWidth / 2.0);

        String svg = String.format(
            "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"20\">\n" +
            "  <linearGradient id=\"b\" x2=\"0\" y2=\"100%%\">\n" +
            "    <stop offset=\"0\" stop-color=\"#bbb\" stop-opacity=\".1\"/>\n" +
            "    <stop offset=\"1\" stop-opacity=\".1\"/>\n" +
            "  </linearGradient>\n" +
            "  <mask id=\"a\">\n" +
            "    <rect width=\"%d\" height=\"20\" rx=\"3\" fill=\"#fff\"/>\n" +
            "  </mask>\n" +
            "  <g mask=\"url(#a)\">\n" +
            "    <rect width=\"%d\" height=\"20\" fill=\"#4b5563\"/>\n" +
            "    <rect x=\"%d\" width=\"%d\" height=\"20\" fill=\"%s\"/>\n" +
            "    <rect width=\"%d\" height=\"20\" fill=\"url(#b)\"/>\n" +
            "  </g>\n" +
            "  <g fill=\"#fff\" text-anchor=\"middle\" font-family=\"DejaVu Sans,Verdana,Geneva,sans-serif\" font-size=\"11\">\n" +
            "    <text x=\"%.1f\" y=\"15\" fill=\"#010101\" fill-opacity=\".3\">PulseWatch</text>\n" +
            "    <text x=\"%.1f\" y=\"14\">PulseWatch</text>\n" +
            "    <text x=\"%.1f\" y=\"15\" fill=\"#010101\" fill-opacity=\".3\">%s</text>\n" +
            "    <text x=\"%.1f\" y=\"14\">%s</text>\n" +
            "  </g>\n" +
            "</svg>",
            totalWidth, totalWidth, labelWidth, labelWidth, valueWidth, color, totalWidth,
            labelTextX, labelTextX, valueTextX, statusText, valueTextX, statusText
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("image/svg+xml"));
        headers.setCacheControl("no-cache, no-store, must-revalidate");
        headers.setPragma("no-cache");
        headers.setExpires(0);

        return new ResponseEntity<>(svg, headers, HttpStatus.OK);
    }
}
