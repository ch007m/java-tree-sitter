PROJECT := tree-sitter
PWD := $(CURDIR)
LIB := $(PWD)/..
SCRIPTS := $(HOME)/.claude/skills/compile-to-wasm/scripts

# Auto-detect OS and arch
UNAME_S := $(shell uname -s)
UNAME_M := $(shell uname -m)

ifeq ($(UNAME_S),Linux)
  OS_NAME := linux
endif
ifeq ($(UNAME_S),Darwin)
  OS_NAME := macos
endif

ifeq ($(UNAME_M),x86_64)
  ARCH_NAME := x86_64
endif
ifeq ($(UNAME_M),aarch64)
  ARCH_NAME := aarch64
endif
ifeq ($(UNAME_M),arm64)
  ARCH_NAME := aarch64
endif

# Source: use the amalgamated lib.c which includes all modules
SOURCES := $(LIB)/src/lib.c

# Include paths
INCLUDES := -I$(LIB)/include -I$(LIB)/src

# Compiler flags
CFLAGS := \
	--target=wasm32-wasip1 \
	-std=c11 \
	-O3 \
	-fno-stack-protector \
	-fvisibility=hidden \
	-DTREE_SITTER_HIDE_SYMBOLS=0 \
	-D_POSIX_C_SOURCE=200809L \
	$(INCLUDES)

# Linker flags: reactor mode, export malloc/free + all ts_* API functions
LDFLAGS := \
	-mexec-model=reactor \
	-Wl,--export=malloc \
	-Wl,--export=free \
	-Wl,--export=calloc \
	-Wl,--export=realloc \
	-Wl,--export=ts_parser_new \
	-Wl,--export=ts_parser_delete \
	-Wl,--export=ts_parser_language \
	-Wl,--export=ts_parser_set_language \
	-Wl,--export=ts_parser_set_included_ranges \
	-Wl,--export=ts_parser_included_ranges \
	-Wl,--export=ts_parser_parse \
	-Wl,--export=ts_parser_parse_with_options \
	-Wl,--export=ts_parser_parse_string \
	-Wl,--export=ts_parser_parse_string_encoding \
	-Wl,--export=ts_parser_reset \
	-Wl,--export=ts_parser_set_logger \
	-Wl,--export=ts_parser_logger \
	-Wl,--export=ts_parser_print_dot_graphs \
	-Wl,--export=ts_tree_copy \
	-Wl,--export=ts_tree_delete \
	-Wl,--export=ts_tree_root_node \
	-Wl,--export=ts_tree_root_node_with_offset \
	-Wl,--export=ts_tree_language \
	-Wl,--export=ts_tree_included_ranges \
	-Wl,--export=ts_tree_edit \
	-Wl,--export=ts_tree_get_changed_ranges \
	-Wl,--export=ts_tree_print_dot_graph \
	-Wl,--export=ts_node_type \
	-Wl,--export=ts_node_symbol \
	-Wl,--export=ts_node_language \
	-Wl,--export=ts_node_grammar_type \
	-Wl,--export=ts_node_grammar_symbol \
	-Wl,--export=ts_node_start_byte \
	-Wl,--export=ts_node_start_point \
	-Wl,--export=ts_node_end_byte \
	-Wl,--export=ts_node_end_point \
	-Wl,--export=ts_node_string \
	-Wl,--export=ts_node_is_null \
	-Wl,--export=ts_node_is_named \
	-Wl,--export=ts_node_is_missing \
	-Wl,--export=ts_node_is_extra \
	-Wl,--export=ts_node_has_changes \
	-Wl,--export=ts_node_has_error \
	-Wl,--export=ts_node_is_error \
	-Wl,--export=ts_node_parse_state \
	-Wl,--export=ts_node_next_parse_state \
	-Wl,--export=ts_node_parent \
	-Wl,--export=ts_node_child_with_descendant \
	-Wl,--export=ts_node_child \
	-Wl,--export=ts_node_field_name_for_child \
	-Wl,--export=ts_node_field_name_for_named_child \
	-Wl,--export=ts_node_child_count \
	-Wl,--export=ts_node_named_child \
	-Wl,--export=ts_node_named_child_count \
	-Wl,--export=ts_node_child_by_field_name \
	-Wl,--export=ts_node_child_by_field_id \
	-Wl,--export=ts_node_next_sibling \
	-Wl,--export=ts_node_prev_sibling \
	-Wl,--export=ts_node_next_named_sibling \
	-Wl,--export=ts_node_prev_named_sibling \
	-Wl,--export=ts_node_first_child_for_byte \
	-Wl,--export=ts_node_first_named_child_for_byte \
	-Wl,--export=ts_node_descendant_count \
	-Wl,--export=ts_node_descendant_for_byte_range \
	-Wl,--export=ts_node_descendant_for_point_range \
	-Wl,--export=ts_node_named_descendant_for_byte_range \
	-Wl,--export=ts_node_named_descendant_for_point_range \
	-Wl,--export=ts_node_edit \
	-Wl,--export=ts_node_eq \
	-Wl,--export=ts_point_edit \
	-Wl,--export=ts_range_edit \
	-Wl,--export=ts_tree_cursor_new \
	-Wl,--export=ts_tree_cursor_delete \
	-Wl,--export=ts_tree_cursor_reset \
	-Wl,--export=ts_tree_cursor_reset_to \
	-Wl,--export=ts_tree_cursor_current_node \
	-Wl,--export=ts_tree_cursor_current_field_name \
	-Wl,--export=ts_tree_cursor_current_field_id \
	-Wl,--export=ts_tree_cursor_goto_parent \
	-Wl,--export=ts_tree_cursor_goto_next_sibling \
	-Wl,--export=ts_tree_cursor_goto_previous_sibling \
	-Wl,--export=ts_tree_cursor_goto_first_child \
	-Wl,--export=ts_tree_cursor_goto_last_child \
	-Wl,--export=ts_tree_cursor_goto_descendant \
	-Wl,--export=ts_tree_cursor_current_descendant_index \
	-Wl,--export=ts_tree_cursor_current_depth \
	-Wl,--export=ts_tree_cursor_goto_first_child_for_byte \
	-Wl,--export=ts_tree_cursor_goto_first_child_for_point \
	-Wl,--export=ts_tree_cursor_copy \
	-Wl,--export=ts_query_new \
	-Wl,--export=ts_query_delete \
	-Wl,--export=ts_query_pattern_count \
	-Wl,--export=ts_query_capture_count \
	-Wl,--export=ts_query_string_count \
	-Wl,--export=ts_query_start_byte_for_pattern \
	-Wl,--export=ts_query_end_byte_for_pattern \
	-Wl,--export=ts_query_predicates_for_pattern \
	-Wl,--export=ts_query_is_pattern_rooted \
	-Wl,--export=ts_query_is_pattern_non_local \
	-Wl,--export=ts_query_is_pattern_guaranteed_at_step \
	-Wl,--export=ts_query_capture_name_for_id \
	-Wl,--export=ts_query_capture_quantifier_for_id \
	-Wl,--export=ts_query_string_value_for_id \
	-Wl,--export=ts_query_disable_capture \
	-Wl,--export=ts_query_disable_pattern \
	-Wl,--export=ts_query_cursor_new \
	-Wl,--export=ts_query_cursor_delete \
	-Wl,--export=ts_query_cursor_exec \
	-Wl,--export=ts_query_cursor_exec_with_options \
	-Wl,--export=ts_query_cursor_did_exceed_match_limit \
	-Wl,--export=ts_query_cursor_match_limit \
	-Wl,--export=ts_query_cursor_set_match_limit \
	-Wl,--export=ts_query_cursor_set_byte_range \
	-Wl,--export=ts_query_cursor_set_point_range \
	-Wl,--export=ts_query_cursor_set_containing_byte_range \
	-Wl,--export=ts_query_cursor_set_containing_point_range \
	-Wl,--export=ts_query_cursor_next_match \
	-Wl,--export=ts_query_cursor_remove_match \
	-Wl,--export=ts_query_cursor_next_capture \
	-Wl,--export=ts_query_cursor_set_max_start_depth \
	-Wl,--export=ts_language_copy \
	-Wl,--export=ts_language_delete \
	-Wl,--export=ts_language_symbol_count \
	-Wl,--export=ts_language_state_count \
	-Wl,--export=ts_language_symbol_for_name \
	-Wl,--export=ts_language_field_count \
	-Wl,--export=ts_language_field_name_for_id \
	-Wl,--export=ts_language_field_id_for_name \
	-Wl,--export=ts_language_supertypes \
	-Wl,--export=ts_language_subtypes \
	-Wl,--export=ts_language_symbol_name \
	-Wl,--export=ts_language_symbol_type \
	-Wl,--export=ts_language_abi_version \
	-Wl,--export=ts_language_metadata \
	-Wl,--export=ts_language_next_state \
	-Wl,--export=ts_language_name \
	-Wl,--export=ts_lookahead_iterator_new \
	-Wl,--export=ts_lookahead_iterator_delete \
	-Wl,--export=ts_lookahead_iterator_reset_state \
	-Wl,--export=ts_lookahead_iterator_reset \
	-Wl,--export=ts_lookahead_iterator_language \
	-Wl,--export=ts_lookahead_iterator_next \
	-Wl,--export=ts_lookahead_iterator_current_symbol \
	-Wl,--export=ts_lookahead_iterator_current_symbol_name \
	-Wl,--export=ts_set_allocator

.PHONY: clean get-versions get-wasisdk get-binaryen build optimize release all test

clean:
	rm -rf target wasi-sdk binaryen versions.env

get-versions:
	@if [ ! -f "$(PWD)/versions.env" ]; then \
		$(SCRIPTS)/resolve-versions.sh > $(PWD)/versions.env; \
	fi

get-wasisdk: get-versions
	@if [ ! -d "$(PWD)/wasi-sdk" ]; then \
		. $(PWD)/versions.env && \
		echo "Downloading WASI SDK $${WASI_SDK_VERSION}..." && \
		curl -sL "$${WASI_SDK_URL}" | tar xz && \
		mv $$(ls -d wasi-sdk-$${WASI_SDK_VERSION}.0-* | head -1) $(PWD)/wasi-sdk; \
	fi

get-binaryen: get-versions
	@if [ ! -d "$(PWD)/binaryen" ]; then \
		. $(PWD)/versions.env && \
		echo "Downloading Binaryen $${BINARYEN_VERSION}..." && \
		curl -sL "$${BINARYEN_URL}" | tar xz && \
		mv binaryen-$${BINARYEN_TAG} $(PWD)/binaryen; \
	fi

build: get-wasisdk
	mkdir -p target
	$(PWD)/wasi-sdk/bin/clang \
		--sysroot=$(PWD)/wasi-sdk/share/wasi-sysroot \
		$(CFLAGS) \
		$(LDFLAGS) \
		-o target/$(PROJECT).wasm \
		$(SOURCES)

optimize: get-binaryen
	$(PWD)/binaryen/bin/wasm-opt -O3 --strip-debug --low-memory-unused \
		--enable-bulk-memory \
		target/$(PROJECT).wasm -o target/$(PROJECT)-opt.wasm

release: build optimize
	cp target/$(PROJECT)-opt.wasm target/$(PROJECT)-final.wasm
	@echo ""
	@echo "Build complete: target/$(PROJECT)-final.wasm"
	@ls -lh target/$(PROJECT)-final.wasm

test: release
	$(SCRIPTS)/verify-wasm.sh target/$(PROJECT)-final.wasm \
		malloc free ts_parser_new ts_parser_delete ts_parser_set_language \
		ts_parser_parse_string ts_tree_root_node ts_tree_delete \
		ts_node_type ts_node_string ts_node_child_count \
		ts_query_new ts_query_delete ts_query_cursor_new

all: release test