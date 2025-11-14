import RiskImageReport from "./riskImage.js";

document.addEventListener("DOMContentLoaded", function () {
    new ReportHandler().init();
});

function ReportHandler() {
    this.overlay = null;

    this.init = function() {
        this.overlay = document.getElementById('reportLoadingOverlay');

        // Initialize risk image report
        new RiskImageReport().init();

        // Add click handlers to all report links
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

            // Extract filename from Content-Disposition header if not provided
            if (!filename) {
                const contentDisposition = response.headers.get('Content-Disposition');
                if (contentDisposition) {
                    const filenameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
                    if (filenameMatch && filenameMatch[1]) {
                        filename = filenameMatch[1].replace(/['"]/g, '');
                    }
                }
            }

            return response.blob();
        })
        .then(function(blob) {
            // Create download link and trigger download
            const downloadUrl = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.style.display = 'none';
            a.href = downloadUrl;
            a.download = filename || 'rapport.pdf';

            document.body.appendChild(a);
            a.click();

            // Cleanup
            window.URL.revokeObjectURL(downloadUrl);
            document.body.removeChild(a);

            self.hideLoading();
        })
        .catch(function(error) {
            console.error('Error downloading report:', error);
            alert('Der opstod en fejl ved download af rapporten. Prøv venligst igen.');
            self.hideLoading();
        });
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