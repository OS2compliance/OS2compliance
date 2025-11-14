import RiskImageReport from "./riskImage.js";

document.addEventListener("DOMContentLoaded", function () {
    new ReportHandler().init();
});

function ReportHandler() {
    this.overlay = null;

    this.init = function() {
        this.overlay = document.getElementById('reportLoadingOverlay');
        new RiskImageReport().init();
        this.attachReportLinkHandlers();
    };

    this.attachReportLinkHandlers = function() {
        const self = this;
        const reportLinks = document.querySelectorAll('.report-link');

        reportLinks.forEach(function(link) {
            link.addEventListener('click', function(e) {
                e.preventDefault();
                const url = link.getAttribute('data-report-url');
                const filename = link.getAttribute('data-filename');

                if (url) {
                    self.downloadReport(url, filename);
                }
            });
        });
    };

    this.downloadReport = function(url, filename) {
        const self = this;

        this.showLoading();

        fetch(url, {
            method: 'GET',
            headers: {
                'Accept': 'application/octet-stream'
            }
        })
        .then(function(response) {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }

            if (!filename) {
                filename = self.extractFilenameFromHeaders(response.headers);
            }

            return response.blob();
        })
        .then(function(blob) {
            self.triggerBlobDownload(blob, filename || 'rapport.pdf');
            self.hideLoading();
        })
        .catch(function(error) {
            toastService.error('Der opstod en fejl ved download af rapporten. Prøv venligst igen.');
            self.hideLoading();
        });
    };

    this.extractFilenameFromHeaders = function(headers) {
        const contentDisposition = headers.get('Content-Disposition');
        if (contentDisposition) {
            const filenameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
            if (filenameMatch && filenameMatch[1]) {
                return filenameMatch[1].replace(/['"]/g, '');
            }
        }
        return null;
    };

    this.triggerBlobDownload = function(blob, filename) {
        const url = window.URL.createObjectURL(blob);
        const downloadLink = document.getElementById('reportDownloadLink');

        downloadLink.href = url;
        downloadLink.download = filename;
        downloadLink.click();

        // Cleanup
        window.URL.revokeObjectURL(url);
        downloadLink.href = '';
    };

    this.showLoading = function() {
        if (this.overlay) {
            this.overlay.classList.remove('d-none');
        }
    };

    this.hideLoading = function() {
        if (this.overlay) {
            this.overlay.classList.add('d-none');
        }
    };
}