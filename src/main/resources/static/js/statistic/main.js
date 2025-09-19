import {renderDashboardChart, renderOverdueChart, renderTaskStatusChart} from "./taskCharts.js";


document.addEventListener("DOMContentLoaded", function() {
    renderDashboardChart();
    renderOverdueChart()
    renderTaskStatusChart()
})