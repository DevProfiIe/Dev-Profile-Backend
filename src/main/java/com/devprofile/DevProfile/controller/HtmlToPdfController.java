package com.devprofile.DevProfile.controller;

import com.devprofile.DevProfile.dto.response.ApiResponse;
import com.devprofile.DevProfile.service.HtmlToPdfConverter;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class HtmlToPdfController {

    private final HtmlToPdfConverter htmlToPdfConverter;

    public HtmlToPdfController(HtmlToPdfConverter htmlToPdfConverter) {
        this.htmlToPdfConverter = htmlToPdfConverter;
    }

    @PostMapping("/convert-html-to-pdf/{userName}")
    public ApiResponse<List<String>> convertHtmlToPdf(@PathVariable String userName, @RequestBody HtmlToPdfRequest request) {
        List<String> pdfUrls = htmlToPdfConverter.convertHtmlToPdf(userName, request.getHtmlPaths());
        if (pdfUrls.isEmpty()) {
            return new ApiResponse<>(false, null, null, "An error occurred during PDF conversion");
        }
        return new ApiResponse<>(true, pdfUrls, null, "PDF conversion successful");
    }

    public static class HtmlToPdfRequest {
        private List<String> htmlPaths;

        public List<String> getHtmlPaths() {
            return htmlPaths;
        }

        public void setHtmlPaths(List<String> htmlPaths) {
            this.htmlPaths = htmlPaths;
        }
    }
}