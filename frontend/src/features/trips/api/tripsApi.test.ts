import { describe, expect, it } from 'vitest';
import { tripsApi } from './tripsApi';

describe('demo trips API', () => {
  it('supports the Phase 1 trip and stop lifecycle', async () => {
    const created = await tripsApi.create({
      title: 'Lisbon test journey',
      country: 'Portugal',
      city: 'Lisbon',
      startDate: '2027-04-10',
      endDate: '2027-04-12',
      visibility: 'PRIVATE',
    });

    const updated = await tripsApi.update(created.id, {
      title: 'Lisbon long weekend',
      description: 'Trams, viewpoints and pastries.',
      country: 'Portugal',
      city: 'Lisbon',
      startDate: '2027-04-10',
      endDate: '2027-04-13',
      visibility: 'PUBLIC',
    });
    expect(updated.title).toBe('Lisbon long weekend');
    expect(updated.visibility).toBe('PUBLIC');

    const stop = await tripsApi.createStop(created.id, created.startDate, {
      name: 'Miradouro da Senhora',
      description: 'Sunset over the tiled roofs.',
      latitude: 38.719,
      longitude: -9.132,
      arrivalTime: '2027-04-10T00:15:00+03:00',
      category: 'LANDMARK',
      rating: 9,
      position: 0,
    });
    expect(stop.day).toBe(1);

    const editedStop = await tripsApi.updateStop(created.id, stop.id, created.startDate, {
      name: 'Miradouro da Senhora do Monte',
      description: 'The best sunset of the weekend.',
      latitude: 38.719,
      longitude: -9.132,
      arrivalTime: '2027-04-10T18:00:00.000Z',
      category: 'LANDMARK',
      rating: 10,
      position: 0,
    });
    expect(editedStop.rating).toBe(10);

    await tripsApi.deleteStop(created.id, stop.id);
    expect((await tripsApi.getById(created.id)).stops).toHaveLength(0);

    await tripsApi.archive(created.id);
    expect((await tripsApi.list()).some(({ id }) => id === created.id)).toBe(false);
  });
});
