import { Component, input } from '@angular/core';
import { EnumLabelPipe } from '../../shared/enum-label.pipe';
import { IncidentStatus, Priority, Severity } from './incident.models';

@Component({
  selector: 'app-severity-badge',
  imports: [EnumLabelPipe],
  template: '<span class="badge" [class]="severity().toLowerCase()">{{ severity() | enumLabel }}</span>',
  styleUrl: './badges.scss',
})
export class SeverityBadge {
  readonly severity = input.required<Severity>();
}

@Component({
  selector: 'app-status-badge',
  imports: [EnumLabelPipe],
  template: '<span class="badge outline" [class]="status().toLowerCase()">{{ status() | enumLabel }}</span>',
  styleUrl: './badges.scss',
})
export class StatusBadge {
  readonly status = input.required<IncidentStatus>();
}

@Component({
  selector: 'app-priority-badge',
  template: `
    @if (priority(); as current) {
      <span class="badge score" [class]="'priority-' + current.level.toLowerCase()" [title]="title(current)">
        {{ current.score }}
      </span>
    } @else {
      <span class="none" aria-label="No priority">–</span>
    }
  `,
  styleUrl: './badges.scss',
})
export class PriorityBadge {
  readonly priority = input<Priority | undefined>(undefined);

  protected title(priority: Priority): string {
    const level = priority.level.charAt(0) + priority.level.slice(1).toLowerCase();
    return `${level} priority, ${priority.score} of 100`;
  }
}
