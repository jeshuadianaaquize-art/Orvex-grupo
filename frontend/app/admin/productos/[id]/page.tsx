"use client";

import { useParams } from "next/navigation";
import { EditorProducto } from "@/components/EditorProducto";

export default function EditarProducto() {
  const { id } = useParams<{ id: string }>();
  return <EditorProducto key={id} id={Number(id)} />;
}
