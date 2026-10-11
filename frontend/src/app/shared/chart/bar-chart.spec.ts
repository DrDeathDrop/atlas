import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BarChart, ChartItem } from './bar-chart';

const items: ChartItem[] = [
  { label: 'Flood', value: 8 },
  { label: 'Wildfire', value: 4 },
  { label: 'Gas leak', value: 0 },
];

describe('BarChart', () => {
  let fixture: ComponentFixture<BarChart>;

  async function render(inputs: Record<string, unknown>): Promise<void> {
    await TestBed.configureTestingModule({ imports: [BarChart] }).compileComponents();
    fixture = TestBed.createComponent(BarChart);
    fixture.componentRef.setInput('caption', 'Incidents by category');
    for (const [name, value] of Object.entries(inputs)) {
      fixture.componentRef.setInput(name, value);
    }
    await fixture.whenStable();
  }

  function element(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  it('draws one bar per item, scaled to the largest value', async () => {
    await render({ items });

    const bars = Array.from(element().querySelectorAll<HTMLElement>('.bar'));
    expect(bars.map((bar) => bar.style.width)).toEqual(['100%', '50%', '0%']);
    expect(element().querySelectorAll('.bar-value')[0].textContent).toBe('8');
    expect(element().querySelector('.bars')!.getAttribute('aria-label')).toBe('Incidents by category');
  });

  it('draws columns with a tooltip for each', async () => {
    await render({ items, layout: 'columns' });

    const columns = Array.from(element().querySelectorAll<HTMLElement>('.column'));
    expect(columns.map((column) => column.style.height)).toEqual(['100%', '50%', '0%']);
    expect(element().querySelectorAll('.tip')[1].textContent).toContain('Wildfire');
    expect(element().querySelector('.axis')!.textContent).toContain('8');
    expect(element().querySelector('.range')!.textContent).toContain('Flood');
    expect(element().querySelector('.range')!.textContent).toContain('Gas leak');
  });

  it('offers the same numbers as a table', async () => {
    await render({ items, categoryLabel: 'Category' });

    const rows = Array.from(element().querySelectorAll('tbody tr')).map((row) =>
      Array.from(row.children).map((cell) => cell.textContent!.trim()),
    );
    expect(rows).toEqual([
      ['Flood', '8'],
      ['Wildfire', '4'],
      ['Gas leak', '0'],
    ]);
    expect(element().querySelector('thead')!.textContent).toContain('Category');
  });

  it('says so when there is nothing to show', async () => {
    await render({ items: [{ label: 'Flood', value: 0 }] });

    expect(element().textContent).toContain('Nothing to show');
    expect(element().querySelector('.bar')).toBeNull();
  });
});
