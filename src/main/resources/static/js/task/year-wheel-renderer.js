var MONTH_NAMES = ['Jan', 'Feb', 'Mar', 'Apr', 'Maj', 'Jun', 'Jul', 'Aug', 'Sep', 'Okt', 'Nov', 'Dec'];

/**
 * Renders the full year wheel: legend, grid and resets detail panel.
 * @param {Object} data - Year wheel data from the backend
 * @param {Object} state - Shared application state
 */
export function renderYearWheel(data, state) {
    state.lastData = data;
    state.activeTagFilters.clear();
    renderLegend(data.tags, state);
    renderGrid(data.months, state);
    hideDetailPanel();
}

// --- Legend ---

function renderLegend(tags, state) {
    var legend = document.getElementById('yearWheelLegend');
    if (!legend) {
        return;
    }

    legend.innerHTML = '';

    var label = document.createElement('span');
    label.className = 'legend-label';
    label.style.cssText = 'font-size: 0.78rem; font-weight: 600; margin-right: 0.25rem;';
    label.textContent = 'Tags:';
    legend.appendChild(label);

    if (tags.length === 0) {
        var noTags = document.createElement('span');
        noTags.className = 'text-muted';
        noTags.style.fontSize = '0.78rem';
        noTags.textContent = 'Ingen årshjul-tags fundet';
        legend.appendChild(noTags);
        return;
    }

    tags.forEach(function (tag) {
        var item = document.createElement('button');
        item.type = 'button';
        item.className = 'legend-filter-btn';
        item.dataset.tagId = tag.id;

        var dot = document.createElement('div');
        dot.className = 'legend-dot';
        dot.style.backgroundColor = tag.color;

        var text = document.createTextNode(tag.value);

        item.appendChild(dot);
        item.appendChild(text);

        item.addEventListener('click', function () {
            var tagId = String(tag.id);

            if (state.activeTagFilters.has(tagId)) {
                state.activeTagFilters.delete(tagId);
                item.classList.remove('active');
            } else {
                state.activeTagFilters.add(tagId);
                item.classList.add('active');
            }

            applyTagFilter(state);
        });

        legend.appendChild(item);
    });
}

// --- Tag Filtering ---

function applyTagFilter(state) {
    var chips = document.querySelectorAll('.task-chip');
    var hasActiveFilters = state.activeTagFilters.size > 0;

    chips.forEach(function (chip) {
        if (!hasActiveFilters) {
            chip.classList.remove('yw-hidden');
            return;
        }

        var chipTagIds = chip.dataset.tagIds ? chip.dataset.tagIds.split(',') : [];
        var matches = chipTagIds.some(function (id) {
            return state.activeTagFilters.has(id);
        });

        if (matches) {
            chip.classList.remove('yw-hidden');
        } else {
            chip.classList.add('yw-hidden');
        }
    });

    // Update empty state message and counter for each month
    var monthCards = document.querySelectorAll('.month-card');
    monthCards.forEach(function (card) {
        var body = card.querySelector('.month-body');
        var visibleChips = body.querySelectorAll('.task-chip:not(.yw-hidden)');
        var emptyMsg = body.querySelector('.month-empty');

        if (visibleChips.length === 0) {
            if (!emptyMsg) {
                emptyMsg = document.createElement('span');
                emptyMsg.className = 'month-empty';
                emptyMsg.textContent = 'Ingen aktiviteter';
                body.appendChild(emptyMsg);
            }
            emptyMsg.classList.remove('yw-hidden');
        } else if (emptyMsg) {
            emptyMsg.classList.add('yw-hidden');
        }

        // Update month counter badge
        var counter = card.querySelector('.month-count');
        if (counter) {
            if (visibleChips.length > 0) {
                counter.textContent = visibleChips.length;
                counter.classList.remove('yw-hidden');
            } else {
                counter.textContent = '';
                counter.classList.add('yw-hidden');
            }
        }
    });

    hideDetailPanel();
}

// --- Grid ---

function renderGrid(months, state) {
    var grid = document.getElementById('yearGrid');
    if (!grid) {
        return;
    }

    grid.innerHTML = '';
    var isCurrentYear = state.currentYear === new Date().getFullYear();

    for (var m = 1; m <= 12; m++) {
        var tasks = months[m] || [];
        var card = document.createElement('div');
        card.className = 'month-card';

        var isCurrent = isCurrentYear && m === state.currentMonth;
        if (isCurrent) {
            card.classList.add('current-month');
        }

        card.appendChild(createMonthHeader(m, isCurrent, tasks.length));
        card.appendChild(createMonthBody(tasks, state));

        grid.appendChild(card);
    }
}

function createMonthHeader(month, isCurrent, taskCount) {
    var header = document.createElement('div');
    header.className = 'month-header';

    var monthName = document.createElement('span');
    monthName.className = 'month-name';
    monthName.textContent = MONTH_NAMES[month - 1];
    header.appendChild(monthName);

    var counter = document.createElement('span');
    counter.className = 'month-count';
    if (taskCount > 0) {
        counter.textContent = taskCount;
    } else {
        counter.classList.add('yw-hidden');
    }
    header.appendChild(counter);

    return header;
}

function createMonthBody(tasks, state) {
    var body = document.createElement('div');
    body.className = 'month-body';

    if (tasks.length === 0) {
        var empty = document.createElement('span');
        empty.className = 'month-empty';
        empty.textContent = 'Ingen aktiviteter';
        body.appendChild(empty);
    } else {
        tasks.forEach(function (task) {
            body.appendChild(createTaskChip(task, state));
        });
    }

    return body;
}

// --- Task Chips ---

function createTaskChip(task, state) {
    var chip = document.createElement('div');
    chip.className = 'task-chip';

    // Use first tag color, fallback to grey
    var tagColor = '#adb5bd';
    if (task.tags && task.tags.length > 0) {
        tagColor = task.tags[0].color;
    }

    chip.style.borderLeftColor = tagColor;
    chip.style.backgroundColor = hexToRgba(tagColor, 0.12);
    chip.style.color = tagColor;

    chip.dataset.taskId = task.id;
    chip.dataset.name = task.name;
    chip.dataset.taskType = task.taskType;
    chip.dataset.repetition = task.repetition;
    chip.dataset.deadline = task.deadline;
    chip.dataset.responsibleNames = task.responsibleNames;
    chip.dataset.responsibleOu = task.responsibleOU;
    chip.dataset.tagColor = tagColor;
    chip.dataset.tags = JSON.stringify(task.tags || []);
    chip.dataset.tagIds = (task.tags || []).map(function (t) { return t.id; }).join(',');
    chip.dataset.status = task.status || 'upcoming';

    // Task name
    var nameSpan = document.createElement('span');
    nameSpan.className = 'task-chip-name';
    nameSpan.textContent = task.name;
    chip.appendChild(nameSpan);

    // Status indicator icon
    if (task.status === 'completed') {
        var completedIcon = document.createElement('i');
        completedIcon.className = 'pli-yes yw-status-icon yw-status-completed';
        completedIcon.title = 'Udført';
        chip.appendChild(completedIcon);
    } else if (task.status === 'overdue') {
        var overdueIcon = document.createElement('i');
        overdueIcon.className = 'pli-exclamation yw-status-icon yw-status-overdue';
        overdueIcon.title = 'Overskredet';
        chip.appendChild(overdueIcon);
    }

    chip.addEventListener('click', function () {
        var prevSelected = document.querySelector('.task-chip.selected');
        if (prevSelected) {
            prevSelected.classList.remove('selected');
        }
        chip.classList.add('selected');
        state.selectedChip = chip;
        showDetailPanel(chip);
    });

    return chip;
}

// --- Detail Panel ---

function showDetailPanel(chip) {
    var panel = document.getElementById('detailPanel');
    if (!panel) {
        return;
    }

    document.getElementById('detailTitle').textContent = chip.dataset.name;
    document.getElementById('detailDeadline').textContent = chip.dataset.deadline;
    document.getElementById('detailRepetition').textContent = chip.dataset.repetition;
    document.getElementById('detailType').textContent = chip.dataset.taskType;
    document.getElementById('detailResponsible').textContent = chip.dataset.responsibleNames;
    document.getElementById('detailDept').textContent = chip.dataset.responsibleOu;
    document.getElementById('detailLink').href = viewUrl + chip.dataset.taskId;

    // Render tag badges
    var tagsContainer = document.getElementById('detailTags');
    tagsContainer.innerHTML = '';
    var tags = JSON.parse(chip.dataset.tags);

    if (tags.length > 0) {
        tags.forEach(function (tag) {
            var badge = document.createElement('span');
            badge.className = 'detail-tag-badge';
            badge.style.backgroundColor = tag.color;
            badge.style.color = tag.contrastColor;
            badge.textContent = tag.value;
            tagsContainer.appendChild(badge);
        });
    } else {
        tagsContainer.textContent = 'Ingen tags';
    }

    // Status badge
    var statusContainer = document.getElementById('detailStatus');
    if (statusContainer) {
        statusContainer.innerHTML = '';
        var status = chip.dataset.status;
        var statusBadge = document.createElement('span');

        if (status === 'completed') {
            statusBadge.className = 'd-inline-block badge bg-success';
            statusBadge.textContent = 'Udført';
        } else if (status === 'overdue') {
            statusBadge.className = 'd-inline-block badge bg-danger';
            statusBadge.textContent = 'Overskredet';
        } else {
            statusBadge.className = 'd-inline-block badge bg-secondary';
            statusBadge.textContent = 'Kommende';
        }

        statusContainer.appendChild(statusBadge);
    }

    // Set border color to match primary tag
    panel.style.borderColor = chip.dataset.tagColor;

    panel.style.display = 'block';
    panel.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

export function hideDetailPanel() {
    var panel = document.getElementById('detailPanel');
    if (panel) {
        panel.style.display = 'none';
    }
    var prevSelected = document.querySelector('.task-chip.selected');
    if (prevSelected) {
        prevSelected.classList.remove('selected');
    }
}

export function initDetailPanelClose() {
    var closeBtn = document.getElementById('detailClose');
    if (closeBtn) {
        closeBtn.addEventListener('click', function () {
            hideDetailPanel();
        });
    }
}

// --- Utility ---

function hexToRgba(hex, alpha) {
    if (!hex || hex.charAt(0) !== '#') {
        return 'rgba(173, 181, 189, ' + alpha + ')';
    }
    var r = parseInt(hex.slice(1, 3), 16);
    var g = parseInt(hex.slice(3, 5), 16);
    var b = parseInt(hex.slice(5, 7), 16);
    return 'rgba(' + r + ', ' + g + ', ' + b + ', ' + alpha + ')';
}