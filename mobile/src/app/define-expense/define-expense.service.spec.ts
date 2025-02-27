import { TestBed } from '@angular/core/testing';

import { DefineExpenseService } from './define-expense.service';

describe('DefineExpenseService', () => {
  let service: DefineExpenseService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(DefineExpenseService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
