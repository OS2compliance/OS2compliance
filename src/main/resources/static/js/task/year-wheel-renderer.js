var MONTH_NAMES = ['Jan', 'Feb', 'Mar', 'Apr', 'Maj', 'Jun', 'Jul', 'Aug', 'Sep', 'Okt', 'Nov', 'Dec'];

// Cached template references (populated on first use)
var templates = {};

/**
 * Returns a cloned DocumentFragment from a <template> element.
 * Caches the template reference for repeated use.
 * @param {string} id - The template element ID
 * @returns {DocumentFragment}
 */
function cloneTemplate(id) {
    if (!templates[id]) {
        templates[id] = document.getElementById(id);
    }
    return templates[id].content.cloneNode(true);
}

/**
 * Returns the first element inside a cloned fragment by data-ref attribute.
 * @param {DocumentFragment|Element} root
 * @param {string} refName
 * @returns {Element}
 */
function ref(root, refName) {
    return root.querySelector('[data-ref="' + refName + '"]');
}

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
    label.textContent = 'Tags:';
    legend.appendChild(label);

    if (tags.length === 0) {
        legend.appendChild(cloneTemplate('tmplNoTags'));
        return;
    }

    tags.forEach(function (tag) {
        var fragment = cloneTemplate('tmplLegendButton');
        var button = fragment.querySelector('.legend-filter-btn');

        button.dataset.tagId = tag.id;
        button.querySelector('.legend-dot').style.setProperty('--tag-color', tag.color);
        ref(fragment, 'label').textContent = tag.value;

        button.addEventListener('click', function () {
            var tagId = String(tag.id);

            if (state.activeTagFilters.has(tagId)) {
                state.activeTagFilters.delete(tagId);
                button.classList.remove('active');
            } else {
                state.activeTagFilters.add(tagId);
                button.classList.add('active');
            }

            applyTagFilter(state);
        });

        legend.appendChild(fragment);
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
                body.appendChild(cloneTemplate('tmplMonthEmpty'));
            } else {
                emptyMsg.classList.remove('yw-hidden');
            }
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
        var fragment = cloneTemplate('tmplMonthCard');
        var card = fragment.querySelector('.month-card');

        var isCurrent = isCurrentYear && m === state.currentMonth;
        if (isCurrent) {
            card.classList.add('current-month');
        }

        // Header
        ref(fragment, 'monthName').textContent = MONTH_NAMES[m - 1];
        var counter = ref(fragment, 'monthCount');
        if (tasks.length > 0) {
            counter.textContent = tasks.length;
            counter.classList.remove('yw-hidden');
        }

        // Body
        var body = ref(fragment, 'monthBody');
        if (tasks.length === 0) {
            body.appendChild(cloneTemplate('tmplMonthEmpty'));
        } else {
            tasks.forEach(function (task) {
                body.appendChild(createTaskChip(task, state));
            });
        }

        grid.appendChild(fragment);
    }
}

// --- Task Chips ---

function createTaskChip(task, state) {
    var fragment = cloneTemplate('tmplTaskChip');
    var chip = fragment.querySelector('.task-chip');

    // Set tag color via CSS custom property (fallback handled in CSS)
    var tagColor = (task.tags && task.tags.length > 0) ? task.tags[0].color : null;
    if (tagColor) {
        chip.style.setProperty('--tag-color', tagColor);
    }

    chip.dataset.taskId = task.id;
    chip.dataset.name = task.name;
    chip.dataset.taskType = task.taskType;
    chip.dataset.repetition = task.repetition;
    chip.dataset.deadline = task.deadline;
    chip.dataset.responsibleNames = task.responsibleNames;
    chip.dataset.responsibleOu = task.responsibleOU;
    chip.dataset.tagColor = tagColor || '#adb5bd';
    chip.dataset.tags = JSON.stringify(task.tags || []);
    chip.dataset.tagIds = (task.tags || []).map(function (t) { return t.id; }).join(',');
    chip.dataset.status = task.status || 'upcoming';

    ref(fragment, 'chipName').textContent = task.name;

    // Status indicator icon
    if (task.status === 'completed') {
        chip.appendChild(cloneTemplate('tmplChipIconCompleted'));
    } else if (task.status === 'overdue') {
        chip.appendChild(cloneTemplate('tmplChipIconOverdue'));
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

    return fragment;
}

// --- Detail Panel ---

var STATUS_CONFIG = {
    completed: { className: 'd-inline-block badge bg-success', text: 'Udført' },
    overdue:   { className: 'd-inline-block badge bg-danger',  text: 'Overskredet' },
    upcoming:  { className: 'd-inline-block badge bg-secondary', text: 'Kommende' }
};

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
            var fragment = cloneTemplate('tmplDetailTagBadge');
            var badge = ref(fragment, 'badge');
            badge.style.setProperty('--badge-bg', tag.color);
            badge.style.setProperty('--badge-color', tag.contrastColor);
            badge.textContent = tag.value;
            tagsContainer.appendChild(fragment);
        });
    } else {
        tagsContainer.textContent = 'Ingen tags';
    }

    // Status badge
    var statusContainer = document.getElementById('detailStatus');
    if (statusContainer) {
        statusContainer.innerHTML = '';
        var config = STATUS_CONFIG[chip.dataset.status] || STATUS_CONFIG.upcoming;
        var fragment = cloneTemplate('tmplStatusBadge');
        var badge = ref(fragment, 'badge');
        badge.className = config.className;
        badge.textContent = config.text;
        statusContainer.appendChild(fragment);
    }

    // Set border color to match primary tag
    panel.style.setProperty('--detail-border-color', chip.dataset.tagColor);

    panel.classList.remove('yw-hidden');
    panel.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

export function hideDetailPanel() {
    var panel = document.getElementById('detailPanel');
    if (panel) {
        panel.classList.add('yw-hidden');
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