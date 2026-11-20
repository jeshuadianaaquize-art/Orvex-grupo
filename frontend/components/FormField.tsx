"use client";

import { cloneElement, isValidElement, useId } from "react";

/** Etiqueta + control + ayuda/error, conectados por id y aria-describedby. */
export function FormField({
  etiqueta,
  ayuda,
  error,
  children,
}: {
  etiqueta: string;
  ayuda?: string;
  error?: string;
  children: React.ReactElement<Record<string, unknown>>;
}) {
  const id = useId();
  const idDesc = `${id}-desc`;
  const control = isValidElement(children)
    ? cloneElement(children, {
        id,
        className: ["entrada", children.props.className].filter(Boolean).join(" "),
        "aria-invalid": error ? true : undefined,
        "aria-describedby": error || ayuda ? idDesc : undefined,
      })
    : children;
  return (
    <div className="campo">
      <label htmlFor={id}>{etiqueta}</label>
      {control}
      {error ? (
        <span id={idDesc} className="error">
          {error}
        </span>
      ) : (
        ayuda && (
          <span id={idDesc} className="ayuda">
            {ayuda}
          </span>
        )
      )}
    </div>
  );
}
