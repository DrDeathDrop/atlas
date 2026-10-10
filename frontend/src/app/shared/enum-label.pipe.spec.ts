import { EnumLabelPipe } from './enum-label.pipe';

describe('EnumLabelPipe', () => {
  const pipe = new EnumLabelPipe();

  it('turns an enum name into readable words', () => {
    expect(pipe.transform('STRUCTURE_FIRE')).toBe('Structure fire');
    expect(pipe.transform('FLOOD')).toBe('Flood');
    expect(pipe.transform('EN_ROUTE')).toBe('En route');
  });

  it('returns an empty string for a missing value', () => {
    expect(pipe.transform(null)).toBe('');
    expect(pipe.transform(undefined)).toBe('');
  });
});
