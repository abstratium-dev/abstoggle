import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { EntityRevision, HistoryChange, HistoryEntry } from '../model.service';
import { Controller } from '../controller';
import { ToastService } from '../core/toast/toast.service';

@Component({
  selector: 'app-history',
  imports: [CommonModule, FormsModule],
  templateUrl: './history.component.html',
  styleUrl: './history.component.scss'
})
export class HistoryComponent implements OnInit {
  private controller = inject(Controller);
  private toastService = inject(ToastService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  entries: HistoryEntry[] = [];
  loading = false;
  error: string | null = null;

  searchTerm = '';
  limit = 50;
  offset = 0;

  selectedEntry: HistoryEntry | null = null;
  detailChanges: HistoryChange[] = [];
  detailLoading = false;
  detailError: string | null = null;

  // Entity-scoped mode: navigated here from another page for a specific entity
  entityMode = false;
  entityModeType: string | null = null;
  entityModeId: string | null = null;
  entityHistory: EntityRevision[] = [];
  entityHistoryLoading = false;
  entityHistoryError: string | null = null;

  readonly REVTYPE_LABELS: Record<number, string> = {
    0: 'ADD',
    1: 'MOD',
    2: 'DEL'
  };

  ngOnInit(): void {
    this.route.queryParamMap.subscribe(params => {
      const entityType = params.get('entityType');
      const entityId = params.get('entityId');
      if (entityType && entityId) {
        this.entityMode = true;
        this.entityModeType = entityType;
        this.entityModeId = entityId;
        this.loadEntityModeHistory();
      } else {
        this.entityMode = false;
        this.entityModeType = null;
        this.entityModeId = null;
        this.loadHistory();
      }
    });
  }

  async loadEntityModeHistory(): Promise<void> {
    if (!this.entityModeType || !this.entityModeId) return;
    this.entityHistoryLoading = true;
    this.entityHistoryError = null;
    this.entityHistory = [];
    try {
      this.entityHistory = await this.controller.getEntityHistory(this.entityModeType, this.entityModeId);
    } catch {
      this.entityHistoryError = 'Failed to load entity history';
    } finally {
      this.entityHistoryLoading = false;
    }
  }

  backToHistory(): void {
    this.router.navigate(['/history']);
  }

  async loadHistory(): Promise<void> {
    this.loading = true;
    this.error = null;
    try {
      this.entries = await this.controller.loadHistory(this.searchTerm || undefined, this.limit, this.offset);
    } catch {
      this.error = 'Failed to load history';
    } finally {
      this.loading = false;
    }
  }

  onSearch(): void {
    this.offset = 0;
    this.selectedEntry = null;
    this.detailChanges = [];
    this.loadHistory();
  }

  onPrevPage(): void {
    if (this.offset >= this.limit) {
      this.offset -= this.limit;
      this.loadHistory();
    }
  }

  onNextPage(): void {
    if (this.entries.length === this.limit) {
      this.offset += this.limit;
      this.loadHistory();
    }
  }

  async selectEntry(entry: HistoryEntry): Promise<void> {
    if (this.selectedEntry?.rev === entry.rev) {
      this.selectedEntry = null;
      this.detailChanges = [];
      return;
    }
    this.selectedEntry = entry;
    this.detailChanges = [];
    this.detailLoading = true;
    this.detailError = null;
    try {
      this.detailChanges = await this.controller.getRevisionDetails(entry.rev);
    } catch {
      this.detailError = 'Failed to load revision details';
    } finally {
      this.detailLoading = false;
    }
  }

  navigateToEntityHistory(table: string, entityId: string): void {
    this.router.navigate(['/history'], { queryParams: { entityType: table, entityId } });
  }

  formatTimestamp(ts: number): string {
    return new Date(ts).toLocaleString();
  }

  revtypeLabel(revtype: number): string {
    return this.REVTYPE_LABELS[revtype] ?? String(revtype);
  }

  revtypeCssClass(revtype: number): string {
    if (revtype === 0) return 'badge-add';
    if (revtype === 1) return 'badge-mod';
    if (revtype === 2) return 'badge-del';
    return '';
  }
}
