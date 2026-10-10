import { Component, input } from '@angular/core';
import { EnumLabelPipe } from '../../shared/enum-label.pipe';
import { IncidentStatus, Severity } from './incident.models';

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
