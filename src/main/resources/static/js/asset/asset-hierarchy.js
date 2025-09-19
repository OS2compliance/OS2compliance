let hierarchyService;

function HierarchyService() {
    let currentChart = null;

    this.init = function(assetId) {
        this.initializeChart();
        this.setupEventListeners();
    };

    this.setupEventListeners = function() {
        const expandAllBtn = document.getElementById('expandAllBtn');
        if (expandAllBtn) {
            expandAllBtn.addEventListener('click', () => {
                this.expandAll();
            });
        }

        const collapseAllBtn = document.getElementById('collapseAllBtn');
        if (collapseAllBtn) {
            collapseAllBtn.addEventListener('click', () => {
                this.collapseAll();
            });
        }

        const fitToViewBtn = document.getElementById('fitToViewBtn');
        if (fitToViewBtn) {
            fitToViewBtn.addEventListener('click', () => {
                this.fitToView();
            });
        }
    };

    this.expandAll = function() {
        if (currentChart && typeof currentChart.expandAll === 'function') {
            currentChart.expandAll();
        }
    };

    this.collapseAll = function() {
        if (currentChart && typeof currentChart.collapseAll === 'function') {
            currentChart.collapseAll();
        }
    };

    this.fitToView = function() {
        if (currentChart && typeof currentChart.fit === 'function') {
            currentChart.fit();
        }
    };

    this.fetchHierarchyData = function(assetId) {
        return new Promise((resolve, reject) => {
            let url = `/rest/assets/${assetId}/hierarchy`;

            fetch(url, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'X-CSRF-TOKEN': token
                }
            })
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                return response.json();
            })
            .then(data => {
                resolve({
                    success: true,
                    data: data
                });
            })
            .catch(error => {
                reject(error);
            });
        });
    };

    this.initializeChart = function() {
        this.fetchHierarchyData(assetId)
        .then(data => {
            if (data && data.success) {
                const allNodes = data.data || [];

                if (allNodes.length === 0) {
                    return;
                }

                this.renderChart(allNodes);

            } else {
                toastService.error('Kunne ikke hente hierarkidata');
            }
        })
        .catch(error => {
            toastService.error('Fejl ved indlæsning af hierarkidiagram: ' + error.message);
        });
    };

    this.transformToD3Format = function(nodes) {
        return nodes.map(node => {
            const isRootNode = node.parentId === null;
            const displayId = node.assetId || node.id;

            return {
                id: node.id,
                parentId: node.parentId,
                name: node.name,
                assetId: displayId,
                isRoot: isRootNode
            };
        });
    };

    this.renderChart = function(nodes) {
        const container = document.getElementById('hierarchy-chart-container');
        try {
            // Clear existing chart
            container.innerHTML = '';

            // Create chart wrapper and chart div
            const chartWrapper = document.createElement('div');
            chartWrapper.className = 'hierarchy-chart-wrapper';

            const chartDiv = document.createElement('div');
            chartDiv.id = 'hierarchy-chart';

            chartWrapper.appendChild(chartDiv);
            container.appendChild(chartWrapper);

            // Transform data for d3-org-chart
            const d3Data = this.transformToD3Format(nodes);

            // Create the org chart
            currentChart = new d3.OrgChart()
                .container('#hierarchy-chart')
                .data(d3Data)
                .svgWidth(1100)
                .svgHeight(700)
                .nodeWidth(d => 200)
                .nodeHeight(d => 80)
                .nodeContent(d => {
                    const isRoot = d.data.isRoot;
                    const borderColor = isRoot ? '#26a69a' : '#25476a';
                    const badgeHtml = isRoot ? '<span style="background-color: #26a69a; color: white; padding: 2px 6px; border-radius: 3px; font-size: 10px; margin-left: 5px;">Rod</span>' : '';

                    return `
                        <div style="
                            background-color: white;
                            border: 2px solid ${borderColor};
                            border-radius: 8px;
                            padding: 12px;
                            font-family: Arial, sans-serif;
                            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                            text-align: center;
                        ">
                            <div style="font-weight: bold; font-size: 12px; margin-bottom: 4px; color: #333;">
                                ${d.data.name}${badgeHtml}
                            </div>
                            <div style="font-size: 10px; color: #666;">
                                ID: ${d.data.assetId}
                            </div>
                        </div>
                    `;
                })
                .render();

            document.getElementById("nodeCount").textContent = "" + nodes.length;

        } catch (error) {
            toastService.error('Fejl ved visning af hierarkidiagram: ' + error.message);
        }
    };
}

document.addEventListener("DOMContentLoaded", function (event) {
    hierarchyService = new HierarchyService();
    hierarchyService.init(assetId);
});