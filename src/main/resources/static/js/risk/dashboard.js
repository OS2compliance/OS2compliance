let networkService, riskMatrixService, riskChangeOverTimeService;

function RiskMatrixService() {
    /**
     * Handle changes to risk matrix filter checkboxes
     * Called when user selects/deselects filter options
     */
    this.onRiskMatrixFilterChange = function() {
        // Get all checked filter values
        const checkedFilters = [];
        const filterCheckboxes = document.querySelectorAll('.risk-matrix-filters input[type="checkbox"]:checked');

        filterCheckboxes.forEach(checkbox => {
            checkedFilters.push(checkbox.value);
        });

        // Clear existing detail list when filters change
        riskMatrixService.clearRiskDetailsList();

        // Update risk matrix based on selected filters
        riskMatrixService.updateRiskMatrix(checkedFilters);
    }

    /**
     * Update the risk matrix display based on selected filters
     * @param {Array} filters - Array of selected filter values
     */
    this.updateRiskMatrix = async function(filters) {
        // Clear existing bubbles
        riskMatrixService.clearMatrixBubbles();

        // Fetch data from backend
        const matrixData = await riskMatrixService.fetchRiskMatrixData(filters);

        // Render bubbles in matrix
        riskMatrixService.renderMatrixBubbles(matrixData);
    }

    /**
     * Clear all existing bubbles from the matrix
     */
    this.clearMatrixBubbles = function() {
        const matrixCells = document.querySelectorAll('.riskScoreField');
        matrixCells.forEach(cell => {
            // Remove existing bubbles
            const existingBubble = cell.querySelector('.risk-bubble');
            if (existingBubble) {
                existingBubble.remove();
            }
        });
    }

    /**
     * Render bubbles in the matrix based on data
     * @param {Array} matrixData - Array of RiskMatrixItem objects
     */
    this.renderMatrixBubbles = function(matrixData) {
        matrixData.forEach(item => {
            const cell = document.querySelector(
                `[data-consequence="${item.consequence}"][data-probability="${item.probability}"]`
            );

            if (cell && item.count > 0) {
                // Create bubble element
                const bubble = document.createElement('div');
                bubble.className = 'risk-bubble';
                bubble.textContent = item.count;

                // Add click handler to show details
                bubble.addEventListener('click', function() {
                    riskMatrixService.showRiskDetails(item.probability, item.consequence, riskMatrixService.getCurrentFilters());
                });

                // Add bubble to cell
                cell.appendChild(bubble);
            }
        });
    }

    /**
     * Show risk details when bubble is clicked
     * @param {number} probability - Probability level
     * @param {number} consequence - Consequence level
     * @param {Array} filters - Current filter selection
     */
    this.showRiskDetails = async function(probability, consequence, filters) {
        const details = await riskMatrixService.fetchRiskDetails(probability, consequence, filters);

        // Get elements
        const detailsSection = document.getElementById('riskDetailsSection');
        const detailsTitle = document.getElementById('riskDetailsTitle');
        const tableBody = document.getElementById('riskDetailsTableBody');
        const emptyMessage = document.getElementById('riskDetailsEmpty');
        const table = document.getElementById('riskDetailsTable');

        // Update title
        detailsTitle.textContent = `Risikovurderinger (Sandsynlighed: ${probability}, Konsekvens: ${consequence})`;

        if (details && details.length > 0) {
            // Clear and populate table
            tableBody.innerHTML = '';
            details.forEach(item => {
                const row = document.createElement('tr');
                row.innerHTML = `
                    <td><a href="/risks/${item.id}">${item.name}</a></td>
                    <td>${item.type}</td>
                    <td>${item.createdAt}</td>
                `;
                tableBody.appendChild(row);
            });

            // Show table, hide empty message
            table.style.display = 'table';
            emptyMessage.style.display = 'none';
        } else {
            // Hide table, show empty message
            table.style.display = 'none';
            emptyMessage.style.display = 'block';
        }

        // Show section
        detailsSection.style.display = 'block';
    }

    /**
     * Clear the risk details list
     */
    this.clearRiskDetailsList = function() {
        const detailsSection = document.getElementById('riskDetailsSection');
        if (detailsSection) {
            detailsSection.style.display = 'none';
        }
    }

    /**
     * Get currently selected filters
     * @returns {Array} Array of selected filter values
     */
    this.getCurrentFilters = function() {
        const checkedFilters = [];
        const filterCheckboxes = document.querySelectorAll('.risk-matrix-filters input[type="checkbox"]:checked');

        filterCheckboxes.forEach(checkbox => {
            checkedFilters.push(checkbox.value);
        });

        return checkedFilters;
    }

    /**
     * Fetch risk matrix data from backend using existing NetworkService
     * @param {Array} filters - Selected filter types
     * @returns {Promise} Matrix data
     */
    this.fetchRiskMatrixData = async function(filters) {
        const params = new URLSearchParams();
        if (filters && filters.length > 0) {
            // Convert filter values to match backend enum names
            const enumFilters = filters.map(filter => {
                switch(filter) {
                    case 'aktiv': return 'ASSET';
                    case 'behandling': return 'REGISTER';
                    case 'scenarie': return 'SCENARIO';
                    default: return filter;
                }
            });
            params.append('types', enumFilters.join(','));
        }

        const url = `/rest/risks/dashboard/risk-matrix?${params}`;
        const data = await networkService.Get(url);

        return data;

    }

    /**
     * Fetch detailed risk items for a specific matrix cell using existing NetworkService
     * @param {number} probability - Probability level (1-4)
     * @param {number} consequence - Consequence level (1-4)
     * @param {Array} filters - Selected filter types
     * @returns {Promise} Risk detail items
     */
    this.fetchRiskDetails = async function(probability, consequence, filters) {
        const params = new URLSearchParams();
        if (filters && filters.length > 0) {
            // Convert filter values to match backend enum names
            const enumFilters = filters.map(filter => {
                switch(filter) {
                    case 'aktiv': return 'ASSET';
                    case 'behandling': return 'REGISTER';
                    case 'scenarie': return 'SCENARIO';
                    default: return filter;
                }
            });
            params.append('types', enumFilters.join(','));
        }

        const url = `/rest/risks/dashboard/risk-matrix/${probability}/${consequence}?${params}`;
        const data = await networkService.Get(url);

        return data;
    }

    /**
     * set colors for matrix
     */
    this.setColors = function() {
        var fields = document.querySelectorAll('.riskScoreField');
        fields.forEach(function(field, index) {
            var consequence = field.dataset.consequence;
            var probability = field.dataset.probability;
            var color = scaleColorMap[consequence + "," + probability];
            riskMatrixService.updateColorFor(field, color);
        });
    }

    this.updateColorFor = function(elem, color) {
        elem.style.backgroundColor = color;
        elem.style.color = foregroundColorForHex(color);
    }
}

function RiskChangeOverTimeService() {
    let chart = null; // Store chart instance

    /**
     * Initialize the development over time section
     */
    this.initialize = function() {
        // Years are already populated via Thymeleaf
        // Load chart for selected year (if any year is selected)
        const selectedYear = document.getElementById('yearSelector').value;
        if (selectedYear) {
            riskChangeOverTimeService.loadChartData(parseInt(selectedYear));
        }
    }

    /**
     * Handle year selection change
     */
    this.onYearChange = function() {
        const selectedYear = document.getElementById('yearSelector').value;
        if (selectedYear) {
            riskChangeOverTimeService.loadChartData(parseInt(selectedYear));
        }
    }

    /**
     * Load and display chart data for selected year
     * @param {number} year - Selected year
     */
    this.loadChartData = async function(year) {
        const chartData = await riskChangeOverTimeService.fetchChartData(year);
        riskChangeOverTimeService.renderChart(chartData);
    }

    /**
     * Render the stacked bar chart
     * @param {Object} chartData - Chart data from backend
     */
    this.renderChart = function(chartData) {
        const ctx = document.getElementById('riskOverTimeChart');

        // Destroy existing chart if it exists
        if (chart) {
            chart.destroy();
        }

        // Helper function to check if array has any non-zero values
        const hasData = (arr) => arr && arr.some(val => val > 0);

        // Build datasets dynamically - only include categories with data
        const datasets = [];

        if (hasData(chartData.green)) {
            datasets.push({
                label: 'Grøn',
                data: chartData.green,
                backgroundColor: '#1DB255',
                borderColor: '#1DB255',
                borderWidth: 1
            });
        }

        if (hasData(chartData.lightGreen)) {
            datasets.push({
                label: 'Lysgrøn',
                data: chartData.lightGreen,
                backgroundColor: '#93D259',
                borderColor: '#93D259',
                borderWidth: 1
            });
        }

        if (hasData(chartData.yellow)) {
            datasets.push({
                label: 'Gul',
                data: chartData.yellow,
                backgroundColor: '#FDFF3C',
                borderColor: '#FDFF3C',
                borderWidth: 1
            });
        }

        if (hasData(chartData.orange)) {
            datasets.push({
                label: 'Orange',
                data: chartData.orange,
                backgroundColor: '#FCC231',
                borderColor: '#FCC231',
                borderWidth: 1
            });
        }

        if (hasData(chartData.red)) {
            datasets.push({
                label: 'Rød',
                data: chartData.red,
                backgroundColor: '#FA0020',
                borderColor: '#FA0020',
                borderWidth: 1
            });
        }

        // Chart.js configuration for stacked bar chart
        const config = {
            type: 'bar',
            data: {
                labels: ['Jan', 'Feb', 'Mar', 'Apr', 'Maj', 'Jun',
                         'Jul', 'Aug', 'Sep', 'Okt', 'Nov', 'Dec'],
                datasets: datasets
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    title: {
                        display: true,
                        text: `Risikofordeling over tid - ${document.getElementById('yearSelector').value}`
                    },
                    legend: {
                        position: 'top'
                    },
                    tooltip: {
                        mode: 'index',
                        intersect: false
                    }
                },
                scales: {
                    x: {
                        stacked: true,
                        title: {
                            display: true,
                            text: 'Måned'
                        }
                    },
                    y: {
                        stacked: true,
                        beginAtZero: true,
                        title: {
                            display: true,
                            text: 'Antal risikovurderinger'
                        }
                    }
                },
                interaction: {
                    mode: 'nearest',
                    axis: 'x',
                    intersect: false
                }
            }
        };

        // Create new chart
        chart = new Chart(ctx, config);
    }

    /**
     * Fetch chart data for specific year
     * @param {number} year - Year to fetch data for
     * @returns {Promise} Chart data
     */
    this.fetchChartData = async function(year) {
        const url = `/rest/risks/dashboard/risk-over-time/${year}`;
        const data = await networkService.Get(url);
        return data;
    }
}

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', function() {
    let token = document.getElementsByName("_csrf")[0].getAttribute("content");
    networkService = new NetworkService(token);
    riskMatrixService = new RiskMatrixService();
    riskChangeOverTimeService = new RiskChangeOverTimeService();

    // Set colors first
    riskMatrixService.setColors();

    // Initialize with default filters (all checked)
    riskMatrixService.onRiskMatrixFilterChange();

    // Initialize development over time section
    riskChangeOverTimeService.initialize();
});