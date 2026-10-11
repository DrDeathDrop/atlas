import { Component, computed, input } from '@angular/core';

export interface ChartItem {
  label: string;
  value: number;
}

@Component({
  selector: 'app-bar-chart',
  templateUrl: './bar-chart.html',
  styleUrl: './bar-chart.scss',
})
export class BarChart {
  readonly items = input<ChartItem[]>([]);
  readonly layout = input<'bars' | 'columns'>('bars');
  readonly caption = input.required<string>();
  readonly valueLabel = input('Incidents');
  readonly categoryLabel = input('Name');

  protected readonly max = computed(() => Math.max(1, ...this.items().map((item) => item.value)));
  protected readonly total = computed(() => this.items().reduce((sum, item) => sum + item.value, 0));
  protected readonly first = computed(() => this.items()[0]?.label ?? '');
  protected readonly last = computed(() => this.items()[this.items().length - 1]?.label ?? '');

  protected share(value: number): number {
    return (value / this.max()) * 100;
  }
}
